package no.nav.syfo.application.narmesteleder.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnService
import no.nav.syfo.narmestelederrelasjon.api.NarmesteLederRelasjonDTO
import no.nav.syfo.narmestelederrelasjon.api.narmesteLederSystemApiV1Path
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import no.nav.syfo.narmestelederrelasjon.kafka.pollAndProcessNarmesteLederRelasjon
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import testhelper.*
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE
import java.time.Duration

class NarmestelederSystemApiLederBytteSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment()
        val database = externalMockEnvironment.database

        application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
        )

        val redisStore = RedisStore(
            redisEnvironment = externalMockEnvironment.environment.redis,
        )

        val eregClient = EregClient(
            clientEnvironment = externalMockEnvironment.environment.clients.ereg,
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

        describe(NarmestelederSystemApiLederBytteSpek::class.java.simpleName) {

            describe("Get list of NarmestelederRelasjon for PersonIdent") {
                val url = narmesteLederSystemApiV1Path
                val validToken = generateJWTAzureAD(
                    externalMockEnvironment.environment.azure.appClientId,
                    testIsdialogmoteClientId,
                    externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                )
                describe("Happy path") {

                    val mockConsumer = mockk<KafkaConsumer<String, String>>()
                    every { mockConsumer.poll(any<Duration>()) } returns generateNarmestelederTestdata()
                    every { mockConsumer.commitSync() } returns Unit
                    it("should return correct list when lederbytte") {
                        val mockConsumer = mockk<KafkaConsumer<String, String>>()
                        every { mockConsumer.poll(any<Duration>()) } returns generateNarmestelederTestdataMedLederBytte()
                        every { mockConsumer.commitSync() } returns Unit
                        runBlocking {
                            pollAndProcessNarmesteLederRelasjon(
                                database = database,
                                kafkaConsumerNarmesteLederRelasjon = mockConsumer,
                            )
                        }

                        verify(exactly = 1) { mockConsumer.commitSync() }

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val narmestelederRelasjonList =
                                objectMapper.readValue<List<NarmesteLederRelasjonDTO>>(response.content!!)

                            val currentRelations =
                                narmestelederRelasjonList.filter { it.status == NarmesteLederRelasjonStatus.INNMELDT_AKTIV.name }
                                    .distinctBy { it.narmesteLederPersonIdentNumber }
                            currentRelations.size shouldBeEqualTo 1
                            currentRelations[0].narmesteLederPersonIdentNumber shouldBeEqualTo NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE.value
                        }
                    }
                }
            }
        }
    }
})
