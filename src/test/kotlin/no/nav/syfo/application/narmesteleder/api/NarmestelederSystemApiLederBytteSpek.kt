package no.nav.syfo.application.narmesteleder.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE
import java.time.Duration

class NarmestelederSystemApiLederBytteSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

    afterEachTest {
        database.dropData()
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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val narmestelederRelasjonList = response.body<List<NarmesteLederRelasjonDTO>>()

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
})
