package no.nav.syfo.application.narmesteleder.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.narmestelederrelasjon.api.*
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import testhelper.*
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.VEILEDER_IDENT

class NarmestelederApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe(NarmestelederApiSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment()

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment,
            )

            describe("Get list of NarmestelederRelasjon for PersonIdent") {
                val url = "$narmesteLederApiV1Path$narmesteLederApiV1PersonIdentPath"
                val validToken = generateJWT(
                    externalMockEnvironment.environment.azureAppClientId,
                    externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                    VEILEDER_IDENT,
                )
                describe("Happy path") {
                    it("should return list of NarmestelederRelasjon if request is successful") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val narmestelederRelasjonList = objectMapper.readValue<List<NarmesteLederRelasjonDTO>>(response.content!!)

                            narmestelederRelasjonList.size shouldBeEqualTo 0
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
                }
            }
        }
    }
})
