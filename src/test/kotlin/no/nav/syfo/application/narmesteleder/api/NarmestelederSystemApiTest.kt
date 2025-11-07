package no.nav.syfo.application.narmesteleder.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
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
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import testhelper.*
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_AKTIV_FOM
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE
import testhelper.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import testhelper.mock.eregOrganisasjonMockResponse
import testhelper.mock.pdlPersonMockRespons
import java.time.Duration

class NarmestelederSystemApiTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val eregClient = EregClient(
        clientEnvironment = externalMockEnvironment.environment.clients.ereg,
        valkeyStore = externalMockEnvironment.cache,
        httpClient = externalMockEnvironment.mockHttpClient,
    )

    private val virksomhetsnavnCronjob = VirksomhetsnavnCronjob(
        eregClient = eregClient,
        virksomhetsnavnService = VirksomhetsnavnService(
            database = database,
        ),
    )

    private val url = narmesteLederSystemApiV1Path
    private val validToken = generateJWTAzureAD(
        externalMockEnvironment.environment.azure.appClientId,
        testIsdialogmoteClientId,
        externalMockEnvironment.wellKnownInternalAzureAD.issuer,
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {

        @Test
        fun `should return list of NarmestelederRelasjon for all historical PersonIdent both as innbygger and leder if request is successful`() =
            runTest {
                val mockConsumer = mockk<KafkaConsumer<String, String>>()
                every { mockConsumer.poll(any<Duration>()) } returns generateNarmestelederTestdata()
                every { mockConsumer.commitSync() } returns Unit

                pollAndProcessNarmesteLederRelasjon(
                    database = database,
                    kafkaConsumerNarmesteLederRelasjon = mockConsumer,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }

                val result = virksomhetsnavnCronjob.virksomhetsnavnJob()

                assertEquals(1, result.failed)
                assertEquals(2, result.updated)

                val result2 = virksomhetsnavnCronjob.virksomhetsnavnJob()

                assertEquals(1, result2.failed)
                assertEquals(0, result2.updated)

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)

                    val narmestelederRelasjonList = response.body<List<NarmesteLederRelasjonDTO>>()

                    assertEquals(2, narmestelederRelasjonList.size)

                    val lederRelasjon =
                        narmestelederRelasjonList.find { it.arbeidstakerPersonIdentNumber == ARBEIDSTAKER_FNR.value }
                            ?: throw NoSuchElementException("Fant ikke leder")
                    val ansattRelasjon =
                        narmestelederRelasjonList.find { it.narmesteLederPersonIdentNumber == ARBEIDSTAKER_FNR.value }
                            ?: throw NoSuchElementException("Fant ikke ansatte")

                    assertEquals(
                        NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE.value,
                        ansattRelasjon.arbeidstakerPersonIdentNumber
                    )
                    assertEquals(ARBEIDSTAKER_FNR.value, ansattRelasjon.narmesteLederPersonIdentNumber)

                    assertEquals(ARBEIDSTAKER_FNR.value, lederRelasjon.arbeidstakerPersonIdentNumber)
                    assertEquals(
                        eregOrganisasjonMockResponse.toEregVirksomhetsnavn().virksomhetsnavn,
                        lederRelasjon.virksomhetsnavn
                    )
                    assertEquals(VIRKSOMHETSNUMMER_DEFAULT.value, lederRelasjon.virksomhetsnummer)
                    assertEquals(NARMESTELEDER_PERSONIDENTNUMBER.value, lederRelasjon.narmesteLederPersonIdentNumber)
                    assertEquals(UserConstants.NARMESTELEDER_TELEFON, lederRelasjon.narmesteLederTelefonnummer)
                    assertEquals(UserConstants.NARMESTELEDER_EPOST, lederRelasjon.narmesteLederEpost)
                    assertEquals(
                        pdlPersonMockRespons.data.hentPersonBolk?.get(0)?.person?.fullName(),
                        lederRelasjon.narmesteLederNavn
                    )
                    assertEquals(NARMESTELEDER_AKTIV_FOM, lederRelasjon.aktivFom)
                    assertNull(lederRelasjon.aktivTom)
                    assertEquals(NarmesteLederRelasjonStatus.INNMELDT_AKTIV.name, lederRelasjon.status)
                }
            }
    }

    @Nested
    @DisplayName("Unhappy paths")
    inner class UnhappyPaths {

        @Test
        fun `should return status Unauthorized if no token is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(url)

                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `should return status BadRequest if no NAV_PERSONIDENT_HEADER is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(url) {
                    bearerAuth(validToken)
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        @Test
        fun `should return status BadRequest if NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(url) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value.drop(1))
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        @Test
        fun `should return status Forbidden if unauthorized AZP is supplied`() {
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

                assertEquals(HttpStatusCode.Forbidden, response.status)
            }
        }
    }
}
