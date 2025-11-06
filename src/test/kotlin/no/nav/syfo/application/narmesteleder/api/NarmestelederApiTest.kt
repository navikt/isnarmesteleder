package no.nav.syfo.application.narmesteleder.api

import com.fasterxml.jackson.databind.ObjectMapper
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
import no.nav.syfo.narmestelederrelasjon.api.narmesteLederApiV1Path
import no.nav.syfo.narmestelederrelasjon.api.narmesteLederApiV1PersonIdentPath
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import no.nav.syfo.narmestelederrelasjon.kafka.NARMESTE_LEDER_RELASJON_TOPIC
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NY_LEDER
import no.nav.syfo.narmestelederrelasjon.kafka.pollAndProcessNarmesteLederRelasjon
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
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

class NarmestelederApiTest {

    private val objectMapper: ObjectMapper = configuredJacksonMapper()
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

    private val url = "$narmesteLederApiV1Path$narmesteLederApiV1PersonIdentPath"
    private val validToken = generateJWTAzureAD(
        externalMockEnvironment.environment.azure.appClientId,
        testSyfomodiapersonClientId,
        externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        VEILEDER_IDENT,
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {

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

        @BeforeEach
        fun beforeEach() {
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
        }

        @Test
        fun `should return list of NarmestelederRelasjon for all historical PersonIdent if request is successful`() =
            runTest {
                pollAndProcessNarmesteLederRelasjon(
                    database = database,
                    kafkaConsumerNarmesteLederRelasjon = mockConsumer,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }

                val result = virksomhetsnavnCronjob.virksomhetsnavnJob()

                assertEquals(1, result.failed)
                assertEquals(1, result.updated)

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
                    assertEquals(1, narmestelederRelasjonList.size)

                    val narmesteLederRelasjonDeaktivert = narmestelederRelasjonList.first()
                    assertEquals(ARBEIDSTAKER_FNR.value, narmesteLederRelasjonDeaktivert.arbeidstakerPersonIdentNumber)
                    assertEquals(
                        eregOrganisasjonMockResponse.toEregVirksomhetsnavn().virksomhetsnavn,
                        narmesteLederRelasjonDeaktivert.virksomhetsnavn
                    )
                    assertEquals(narmesteLederLeesah.orgnummer, narmesteLederRelasjonDeaktivert.virksomhetsnummer)
                    assertEquals(
                        narmesteLederLeesah.narmesteLederFnr,
                        narmesteLederRelasjonDeaktivert.narmesteLederPersonIdentNumber
                    )
                    assertEquals(
                        narmesteLederLeesah.narmesteLederTelefonnummer,
                        narmesteLederRelasjonDeaktivert.narmesteLederTelefonnummer
                    )
                    assertEquals(
                        narmesteLederLeesah.narmesteLederEpost,
                        narmesteLederRelasjonDeaktivert.narmesteLederEpost
                    )
                    assertEquals(
                        pdlPersonMockRespons.data.hentPersonBolk?.get(0)?.person?.fullName(),
                        narmesteLederRelasjonDeaktivert.narmesteLederNavn
                    )
                    assertEquals(narmesteLederLeesah.aktivFom, narmesteLederRelasjonDeaktivert.aktivFom)
                    assertEquals(narmesteLederLeesah.aktivTom, narmesteLederRelasjonDeaktivert.aktivTom)
                    assertEquals(
                        NarmesteLederRelasjonStatus.INNMELDT_AKTIV.name,
                        narmesteLederRelasjonDeaktivert.status
                    )
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
        fun `should return status Forbidden if denied access to personident supplied in NAV_PERSONIDENT_HEADER`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(url) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_VEILEDER_NO_ACCESS.value)
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
            }
        }
    }
}
