package no.nav.syfo.application.narmesteleder.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.ereg.toEregVirksomhetsnavn
import no.nav.syfo.client.pdl.domain.fullName
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnService
import no.nav.syfo.narmestelederrelasjon.api.NarmesteLederRelasjonDTO
import no.nav.syfo.narmestelederrelasjon.api.narmesteLederSystemApiV1Path
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import no.nav.syfo.narmestelederrelasjon.kafka.NARMESTE_LEDER_RELASJON_TOPIC
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NY_LEDER
import no.nav.syfo.narmestelederrelasjon.kafka.pollAndProcessNarmesteLederRelasjon
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import redis.clients.jedis.*
import testhelper.*
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.ARBEIDSTAKER_NO_VIRKSOMHETNAVN
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE
import testhelper.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN
import testhelper.generator.generateNarmesteLederLeesah
import testhelper.mock.toHistoricalPersonIdentNumber
import java.time.Duration
import java.time.OffsetDateTime

class NarmestelederSystemApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment()
        val database = externalMockEnvironment.database

        application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
        )

        val redisStore = RedisStore(
            jedisPool = JedisPool(
                JedisPoolConfig(),
                externalMockEnvironment.environment.redisHost,
                externalMockEnvironment.environment.redisPort,
                Protocol.DEFAULT_TIMEOUT,
                externalMockEnvironment.environment.redisSecret,
            ),
        )

        val azureAdClient = AzureAdClient(
            azureAppClientId = externalMockEnvironment.environment.azureAppClientId,
            azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
            azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
            redisStore = redisStore,
        )

        val eregClient = EregClient(
            azureAdClient = azureAdClient,
            isproxyClientId = externalMockEnvironment.environment.isproxyClientId,
            baseUrl = externalMockEnvironment.environment.isproxyUrl,
            redisStore = redisStore,
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

        beforeGroup {
            externalMockEnvironment.startExternalMocks()
        }

        afterGroup {
            externalMockEnvironment.stopExternalMocks()
        }

        describe(NarmestelederSystemApiSpek::class.java.simpleName) {

            describe("Get list of NarmestelederRelasjon for PersonIdent") {
                val url = narmesteLederSystemApiV1Path
                val validToken = generateJWT(
                    externalMockEnvironment.environment.azureAppClientId,
                    testSyfomoteadminClientId,
                    externalMockEnvironment.wellKnownInternalAzureAD.issuer,
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
                    val ansattLeesah = generateNarmesteLederLeesah(
                        arbeidstakerPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
                        narmestelederPersonIdentNumber = ARBEIDSTAKER_FNR.toHistoricalPersonIdentNumber(),
                        status = null,
                        timestamp = OffsetDateTime.now().minusDays(1),
                    )
                    val ansattLeesahRecord = ConsumerRecord(
                        NARMESTE_LEDER_RELASJON_TOPIC,
                        partition,
                        3,
                        "something",
                        objectMapper.writeValueAsString(ansattLeesah),
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
                                ansattLeesahRecord,
                            )
                        )
                    )
                    every { mockConsumer.commitSync() } returns Unit

                    it("should return list of NarmestelederRelasjon for all historical PersonIdent both as innbygger and leder if request is successful") {
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
                            result.updated shouldBeEqualTo 2
                        }

                        runBlocking {
                            val result = virksomhetsnavnCronjob.virksomhetsnavnJob()

                            result.failed shouldBeEqualTo 1
                            result.updated shouldBeEqualTo 0
                        }

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val narmestelederRelasjonList =
                                objectMapper.readValue<List<NarmesteLederRelasjonDTO>>(response.content!!)

                            narmestelederRelasjonList.size shouldBeEqualTo 2

                            val lederRelasjon =
                                narmestelederRelasjonList.find { it.arbeidstakerPersonIdentNumber == ARBEIDSTAKER_FNR.value }
                                    ?: throw NoSuchElementException("Fant ikke leder")
                            val ansattRelasjon =
                                narmestelederRelasjonList.find { it.narmesteLederPersonIdentNumber == ARBEIDSTAKER_FNR.value }
                                    ?: throw NoSuchElementException("Fant ikke ansatte")

                            ansattRelasjon.arbeidstakerPersonIdentNumber shouldBeEqualTo NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE.value
                            ansattRelasjon.narmesteLederPersonIdentNumber shouldBeEqualTo ARBEIDSTAKER_FNR.value

                            lederRelasjon.arbeidstakerPersonIdentNumber shouldBeEqualTo ARBEIDSTAKER_FNR.value
                            lederRelasjon.virksomhetsnavn shouldBeEqualTo externalMockEnvironment.isproxyMock.eregOrganisasjonResponse.toEregVirksomhetsnavn().virksomhetsnavn
                            lederRelasjon.virksomhetsnummer shouldBeEqualTo narmesteLederLeesah.orgnummer
                            lederRelasjon.narmesteLederPersonIdentNumber shouldBeEqualTo narmesteLederLeesah.narmesteLederFnr
                            lederRelasjon.narmesteLederTelefonnummer shouldBeEqualTo narmesteLederLeesah.narmesteLederTelefonnummer
                            lederRelasjon.narmesteLederEpost shouldBeEqualTo narmesteLederLeesah.narmesteLederEpost
                            lederRelasjon.narmesteLederNavn shouldBeEqualTo externalMockEnvironment.pdlMock.respons.data.hentPersonBolk?.get(
                                0
                            )?.person?.fullName()
                            lederRelasjon.aktivFom shouldBeEqualTo narmesteLederLeesah.aktivFom
                            lederRelasjon.aktivTom shouldBeEqualTo narmesteLederLeesah.aktivTom
                            lederRelasjon.status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_AKTIV.name
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
                                addHeader(Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }

                    it("should return status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value.drop(1))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }
                    it("should return status Forbidden if unauthorized AZP is supplied") {
                        val validTokenUnauthorizedAZP = generateJWT(
                            externalMockEnvironment.environment.azureAppClientId,
                            "unauthorizedId",
                            externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                        )

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(Authorization, bearerHeader(validTokenUnauthorizedAZP))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
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
