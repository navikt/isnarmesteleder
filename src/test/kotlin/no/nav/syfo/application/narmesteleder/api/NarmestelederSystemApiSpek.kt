package no.nav.syfo.application.narmesteleder.api

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
import no.nav.syfo.narmestelederrelasjon.api.narmesteLederSystemApiV1Path
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import no.nav.syfo.narmestelederrelasjon.kafka.pollAndProcessNarmesteLederRelasjon
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import testhelper.*
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_AKTIV_FOM
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE
import testhelper.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import testhelper.mock.eregOrganisasjonMockResponse
import testhelper.mock.pdlPersonMockRespons
import java.time.Duration

class NarmestelederSystemApiSpek : Spek({
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

    describe(NarmestelederSystemApiSpek::class.java.simpleName) {

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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val narmestelederRelasjonList = response.body<List<NarmesteLederRelasjonDTO>>()

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
                        lederRelasjon.virksomhetsnavn shouldBeEqualTo eregOrganisasjonMockResponse.toEregVirksomhetsnavn().virksomhetsnavn
                        lederRelasjon.virksomhetsnummer shouldBeEqualTo VIRKSOMHETSNUMMER_DEFAULT.value
                        lederRelasjon.narmesteLederPersonIdentNumber shouldBeEqualTo NARMESTELEDER_PERSONIDENTNUMBER.value
                        lederRelasjon.narmesteLederTelefonnummer shouldBeEqualTo UserConstants.NARMESTELEDER_TELEFON
                        lederRelasjon.narmesteLederEpost shouldBeEqualTo UserConstants.NARMESTELEDER_EPOST
                        lederRelasjon.narmesteLederNavn shouldBeEqualTo pdlPersonMockRespons.data.hentPersonBolk?.get(
                            0
                        )?.person?.fullName()
                        lederRelasjon.aktivFom shouldBeEqualTo NARMESTELEDER_AKTIV_FOM
                        lederRelasjon.aktivTom shouldBeEqualTo null
                        lederRelasjon.status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_AKTIV.name
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
                it("should return status Forbidden if unauthorized AZP is supplied") {
                    val validTokenUnauthorizedAZP = generateJWTAzureAD(
                        externalMockEnvironment.environment.azure.appClientId,
                        "unauthorizedId",
                        externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                    )

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validTokenUnauthorizedAZP)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Forbidden
                    }
                }
            }
        }
    }
})
