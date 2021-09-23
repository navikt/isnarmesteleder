package no.nav.syfo.application.narmesteleder.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.narmestelederrelasjon.api.*
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import no.nav.syfo.narmestelederrelasjon.kafka.NARMESTE_LEDER_RELASJON_TOPIC
import no.nav.syfo.narmestelederrelasjon.kafka.pollAndProcessNarmesteLederRelasjon
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import testhelper.*
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import testhelper.UserConstants.VEILEDER_IDENT
import testhelper.generator.generateNarmesteLederLeesah
import java.time.Duration

class NarmestelederApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment()
        val database = externalMockEnvironment.database

        application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
        )

        afterEachTest {
            database.dropData()
        }

        beforeGroup {
            externalMockEnvironment.startExternalMocks()
        }

        afterGroup {
            externalMockEnvironment.stopExternalMocks()
        }

        describe(NarmestelederApiSpek::class.java.simpleName) {

            describe("Get list of NarmestelederRelasjon for PersonIdent") {
                val url = "$narmesteLederApiV1Path$narmesteLederApiV1PersonIdentPath"
                val validToken = generateJWT(
                    externalMockEnvironment.environment.azureAppClientId,
                    externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                    VEILEDER_IDENT,
                )
                describe("Happy path") {
                    val partition = 0
                    val narmesteLederRelasjonTopicPartition = TopicPartition(
                        NARMESTE_LEDER_RELASJON_TOPIC,
                        partition,
                    )
                    val narmesteLederLeesah = generateNarmesteLederLeesah()
                    val narmesteLederLeesahRecord = ConsumerRecord(
                        NARMESTE_LEDER_RELASJON_TOPIC,
                        partition,
                        1,
                        "something",
                        objectMapper.writeValueAsString(narmesteLederLeesah),
                    )
                    val narmesteLederLeesahRecordDuplicate = ConsumerRecord(
                        NARMESTE_LEDER_RELASJON_TOPIC,
                        partition,
                        1,
                        "something",
                        objectMapper.writeValueAsString(narmesteLederLeesah),
                    )
                    val mockConsumer = mockk<KafkaConsumer<String, String>>()
                    every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                        mapOf(
                            narmesteLederRelasjonTopicPartition to listOf(
                                narmesteLederLeesahRecord,
                                narmesteLederLeesahRecordDuplicate,
                            )
                        )
                    )
                    every { mockConsumer.commitSync() } returns Unit

                    it("should return list of NarmestelederRelasjon if request is successful") {
                        runBlocking {
                            pollAndProcessNarmesteLederRelasjon(
                                database = database,
                                kafkaConsumerNarmesteLederRelasjon = mockConsumer,
                            )
                        }

                        verify(exactly = 1) { mockConsumer.commitSync() }

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val narmestelederRelasjonList = objectMapper.readValue<List<NarmesteLederRelasjonDTO>>(response.content!!)

                            narmestelederRelasjonList.size shouldBeEqualTo 1

                            val narmesteLederRelasjon = narmestelederRelasjonList.first()
                            narmesteLederRelasjon.arbeidstakerPersonIdentNumber shouldBeEqualTo narmesteLederLeesah.fnr
                            narmesteLederRelasjon.virksomhetsnavn.shouldBeNull()
                            narmesteLederRelasjon.virksomhetsnummer shouldBeEqualTo narmesteLederLeesah.orgnummer
                            narmesteLederRelasjon.narmesteLederPersonIdentNumber shouldBeEqualTo narmesteLederLeesah.narmesteLederFnr
                            narmesteLederRelasjon.narmesteLederTelefonnummer shouldBeEqualTo narmesteLederLeesah.narmesteLederTelefonnummer
                            narmesteLederRelasjon.narmesteLederEpost shouldBeEqualTo narmesteLederLeesah.narmesteLederEpost
                            narmesteLederRelasjon.aktivFom shouldBeEqualTo narmesteLederLeesah.aktivFom
                            narmesteLederRelasjon.aktivTom shouldBeEqualTo narmesteLederLeesah.aktivTom
                            narmesteLederRelasjon.status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_AKTIV.name
                        }
                    }
                }

                describe("Unhappy paths") {
                    it("should return status Unauthorized if no token is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {}
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
                        }
                    }

                    it("should return status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }

                    it("should return status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value.drop(1))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }
                    it("should return status Forbidden if denied access to personident supplied in $NAV_PERSONIDENT_HEADER") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_VEILEDER_NO_ACCESS.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Forbidden
                        }
                    }
                }
            }
        }
    }
})
