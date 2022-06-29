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
import no.nav.syfo.narmestelederrelasjon.api.*
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
import testhelper.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import java.time.Duration

class NarmestelederSelvbetjeningApiSpek : Spek({
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

        val azureAdClient = AzureAdClient(
            azureEnviroment = externalMockEnvironment.environment.azure,
            redisStore = redisStore,
        )

        val eregClient = EregClient(
            azureAdClient = azureAdClient,
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

        describe(NarmestelederSelvbetjeningApiSpek::class.java.simpleName) {

            describe("Get list of NarmestelederRelasjon for PersonIdent") {
                val url = narmesteLederSelvbetjeningApiV1Path
                val validToken = generateJWTTokenx(
                    audience = externalMockEnvironment.environment.tokenx.tokenxClientId,
                    clientId = "dev-gcp:teamsykefravr:isdialogmote",
                    issuer = externalMockEnvironment.wellKnownSelvbetjening.issuer,
                    subject = ARBEIDSTAKER_FNR.value,
                )

                describe("Happy path") {

                    val mockConsumer = mockk<KafkaConsumer<String, String>>()
                    every { mockConsumer.poll(any<Duration>()) } returns generateNarmestelederTestdata()
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
                            lederRelasjon.virksomhetsnavn shouldBeEqualTo externalMockEnvironment.eregMock.eregOrganisasjonResponse.toEregVirksomhetsnavn().virksomhetsnavn
                            lederRelasjon.virksomhetsnummer shouldBeEqualTo VIRKSOMHETSNUMMER_DEFAULT.value
                            lederRelasjon.narmesteLederPersonIdentNumber shouldBeEqualTo UserConstants.NARMESTELEDER_PERSONIDENTNUMBER.value
                            lederRelasjon.narmesteLederTelefonnummer shouldBeEqualTo UserConstants.NARMESTELEDER_TELEFON
                            lederRelasjon.narmesteLederEpost shouldBeEqualTo UserConstants.NARMESTELEDER_EPOST
                            lederRelasjon.narmesteLederNavn shouldBeEqualTo externalMockEnvironment.pdlMock.respons.data.hentPersonBolk?.get(
                                0
                            )?.person?.fullName()
                            lederRelasjon.aktivFom shouldBeEqualTo UserConstants.NARMESTELEDER_AKTIV_FOM
                            lederRelasjon.aktivTom shouldBeEqualTo null
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

                    it("should return status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                        val validTokenInvalidSubject = generateJWTTokenx(
                            audience = externalMockEnvironment.environment.tokenx.tokenxClientId,
                            clientId = "dev-gcp:teamsykefravr:isdialogmote",
                            issuer = externalMockEnvironment.wellKnownSelvbetjening.issuer,
                            subject = ARBEIDSTAKER_FNR.value.drop(1),
                        )
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(Authorization, bearerHeader(validTokenInvalidSubject))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }
    }
})
