package no.nav.syfo.application.narmesteleder.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.syfo.narmestelederrelasjon.api.NarmesteLederRelasjonDTO
import no.nav.syfo.narmestelederrelasjon.api.narmesteLederSystemApiV1Path
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import no.nav.syfo.narmestelederrelasjon.kafka.pollAndProcessNarmesteLederRelasjon
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import testhelper.*
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE
import java.time.Duration

class NarmestelederSystemApiLederBytteTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

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

    @Test
    fun `should return correct list when lederbytte`() = runTest {
        val mockConsumer = mockk<KafkaConsumer<String, String>>()
        every { mockConsumer.poll(any<Duration>()) } returns generateNarmestelederTestdataMedLederBytte()
        every { mockConsumer.commitSync() } returns Unit

        pollAndProcessNarmesteLederRelasjon(
            database = database,
            kafkaConsumerNarmesteLederRelasjon = mockConsumer,
        )

        verify(exactly = 1) { mockConsumer.commitSync() }

        testApplication {
            val client = setupApiAndClient()
            val response = client.get(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val narmestelederRelasjonList = response.body<List<NarmesteLederRelasjonDTO>>()

            val currentRelations =
                narmestelederRelasjonList.filter { it.status == NarmesteLederRelasjonStatus.INNMELDT_AKTIV.name }
                    .distinctBy { it.narmesteLederPersonIdentNumber }
            assertEquals(1, currentRelations.size)
            assertEquals(
                NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE.value,
                currentRelations[0].narmesteLederPersonIdentNumber
            )
        }
    }
}
