package no.nav.syfo.application.narmesteleder.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.ereg.toEregVirksomhetsnavn
import no.nav.syfo.client.pdl.domain.fullName
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnService
import no.nav.syfo.narmestelederrelasjon.api.NarmesteLederRelasjonDTO
import no.nav.syfo.narmestelederrelasjon.api.narmesteLederApiV1Path
import no.nav.syfo.narmestelederrelasjon.api.narmesteLederApiV1PersonIdentPath
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import no.nav.syfo.narmestelederrelasjon.kafka.NARMESTE_LEDER_RELASJON_TOPIC
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NY_LEDER
import no.nav.syfo.narmestelederrelasjon.kafka.pollAndProcessNarmesteLederRelasjon
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import testhelper.*
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.ARBEIDSTAKER_NO_VIRKSOMHETNAVN
import testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import testhelper.UserConstants.VEILEDER_IDENT
import testhelper.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN
import testhelper.generator.generateNarmesteLederLeesah
import testhelper.mock.eregOrganisasjonMockResponse
import testhelper.mock.pdlPersonMockRespons
import testhelper.mock.toHistoricalPersonIdentNumber
import java.time.Duration
import java.time.OffsetDateTime

class NarmestelederApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

    val eregClient = EregClient(
        clientEnvironment = externalMockEnvironment.environment.clients.ereg,
        valkeyStore = externalMockEnvironment.cache,
        httpClient = externalMockEnvironment.mockHttpClient,
    )

    val virksomhetsnavnCronjob = VirksomhetsnavnCronjob(
        eregClient = eregClient,
        virksomhetsnavnService = VirksomhetsnavnService(
            database = database,
        ),
    )

    afterEachTest {
        database.dropData()
    }

    describe(NarmestelederApiSpek::class.java.simpleName) {

        describe("Get list of NarmestelederRelasjon for PersonIdent") {
            val url = "$narmesteLederApiV1Path$narmesteLederApiV1PersonIdentPath"
            val validToken = generateJWTAzureAD(
                externalMockEnvironment.environment.azure.appClientId,
                testSyfomodiapersonClientId,
                externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                VEILEDER_IDENT,
            )
            describe("Happy path") {
                val partition = 0
                val narmesteLederRelasjonTopicPartition = TopicPartition(
                    NARMESTE_LEDER_RELASJON_TOPIC,
                    partition,
                )
                val narmesteLederLeesah = generateNarmesteLederLeesah(
                    arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR.toHistoricalPersonIdentNumber(),
                    status = null,
                    timestamp = OffsetDateTime.now().minusDays(1),
                )
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
                val narmesteLederLeesahNoVirksomhetsnavn = generateNarmesteLederLeesah(
                    arbeidstakerPersonIdentNumber = ARBEIDSTAKER_NO_VIRKSOMHETNAVN,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN,
                    status = NY_LEDER,
                )
                val narmesteLederLeesahRecordNoVirksomhetsnavn = ConsumerRecord(
                    NARMESTE_LEDER_RELASJON_TOPIC,
                    partition,
                    2,
                    "something",
                    objectMapper.writeValueAsString(narmesteLederLeesahNoVirksomhetsnavn),
                )
                val mockConsumer = mockk<KafkaConsumer<String, String>>()
                every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        narmesteLederRelasjonTopicPartition to listOf(
                            narmesteLederLeesahRecord,
                            narmesteLederLeesahRecordDuplicate,
                            narmesteLederLeesahRecordNoVirksomhetsnavn,
                        )
                    )
                )
                every { mockConsumer.commitSync() } returns Unit

                it("should return list of NarmestelederRelasjon for all historical PersonIdent if request is successful") {
                    runBlocking {
                        pollAndProcessNarmesteLederRelasjon(
                            database = database,
                            kafkaConsumerNarmesteLederRelasjon = mockConsumer,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }

                    runBlocking {
                        val result = virksomhetsnavnCronjob.virksomhetsnavnJob()

                        result.failed shouldBeEqualTo 1
                        result.updated shouldBeEqualTo 1
                    }

                    runBlocking {
                        val result = virksomhetsnavnCronjob.virksomhetsnavnJob()

                        result.failed shouldBeEqualTo 1
                        result.updated shouldBeEqualTo 0
                    }

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val narmestelederRelasjonList = response.body<List<NarmesteLederRelasjonDTO>>()
                        narmestelederRelasjonList.size shouldBeEqualTo 1

                        val narmesteLederRelasjonDeaktivert = narmestelederRelasjonList.first()
                        narmesteLederRelasjonDeaktivert.arbeidstakerPersonIdentNumber shouldBeEqualTo ARBEIDSTAKER_FNR.value
                        narmesteLederRelasjonDeaktivert.virksomhetsnavn shouldBeEqualTo eregOrganisasjonMockResponse.toEregVirksomhetsnavn().virksomhetsnavn
                        narmesteLederRelasjonDeaktivert.virksomhetsnummer shouldBeEqualTo narmesteLederLeesah.orgnummer
                        narmesteLederRelasjonDeaktivert.narmesteLederPersonIdentNumber shouldBeEqualTo narmesteLederLeesah.narmesteLederFnr
                        narmesteLederRelasjonDeaktivert.narmesteLederTelefonnummer shouldBeEqualTo narmesteLederLeesah.narmesteLederTelefonnummer
                        narmesteLederRelasjonDeaktivert.narmesteLederEpost shouldBeEqualTo narmesteLederLeesah.narmesteLederEpost
                        narmesteLederRelasjonDeaktivert.narmesteLederNavn shouldBeEqualTo pdlPersonMockRespons.data.hentPersonBolk?.get(
                            0
                        )?.person?.fullName()
                        narmesteLederRelasjonDeaktivert.aktivFom shouldBeEqualTo narmesteLederLeesah.aktivFom
                        narmesteLederRelasjonDeaktivert.aktivTom shouldBeEqualTo narmesteLederLeesah.aktivTom
                        narmesteLederRelasjonDeaktivert.status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_AKTIV.name
                    }
                }
            }

            describe("Unhappy paths") {
                it("should return status Unauthorized if no token is supplied") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url)

                        response.status shouldBeEqualTo HttpStatusCode.Unauthorized
                    }
                }

                it("should return status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }

                it("should return status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value.drop(1))
                        }

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }
                it("should return status Forbidden if denied access to personident supplied in $NAV_PERSONIDENT_HEADER") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_VEILEDER_NO_ACCESS.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Forbidden
                    }
                }
            }
        }
    }
})
