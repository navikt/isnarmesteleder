package no.nav.syfo.client.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.*
import no.nav.syfo.client.pdl.PdlClient.Companion.CACHE_PDL_PERSONIDENT_NAME_KEY_PREFIX
import no.nav.syfo.client.pdl.PdlClient.Companion.CACHE_PDL_PERSONIDENT_NAME_TIME_TO_LIVE_SECONDS
import no.nav.syfo.client.pdl.domain.fullName
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import testhelper.ExternalMockEnvironment
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.mock.pdlPersonMockRespons

class PdlClientSpek : Spek({

    val objectMapper: ObjectMapper = configuredJacksonMapper()

    val anyCallId = "callId"

    describe(PdlClientSpek::class.java.simpleName) {

        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val azureAdClientMock = mockk<AzureAdClient>(relaxed = true)
            val redisStoreMock = mockk<RedisStore>(relaxed = true)

            val pdlClientId = "pdlClientId"

            val client = PdlClient(
                azureAdClient = azureAdClientMock,
                clientEnvironment = externalMockEnvironment.environment.clients.pdl,
                redisStore = redisStoreMock,
                httpClient = externalMockEnvironment.mockHttpClient,
            )

            val pdlHentPersonIdent = NARMESTELEDER_PERSONIDENTNUMBER
            val pdlHentPersonName = pdlPersonMockRespons.data.hentPersonBolk?.first()?.person?.fullName() ?: ""

            val pdlPersonidentNameCacheKey = "$CACHE_PDL_PERSONIDENT_NAME_KEY_PREFIX${pdlHentPersonIdent.value}"

            val pdlPersonidentNameCache = PdlPersonidentNameCache(
                name = pdlHentPersonName,
                personIdent = pdlHentPersonIdent.value,
            )

            beforeEachTest {
                clearMocks(redisStoreMock)
            }

            describe("Get name") {
                it("returns cached PdlPersonidentName") {
                    every {
                        redisStoreMock.get(keyList = listOf(pdlPersonidentNameCacheKey),)
                    } returns listOf(objectMapper.writeValueAsString(pdlPersonidentNameCache))

                    runBlocking {
                        client.personIdentNumberNavnMap(
                            callId = anyCallId,
                            personIdentNumberList = listOf(NARMESTELEDER_PERSONIDENTNUMBER)
                        ) shouldBeEqualTo mapOf(pdlHentPersonIdent.value to pdlHentPersonName)
                    }
                    verify(exactly = 1) {
                        redisStoreMock.getObjectList(
                            classType = PdlPersonidentNameCache::class,
                            keyList = listOf(pdlPersonidentNameCacheKey),
                        )
                    }
                    verify(exactly = 0) {
                        redisStoreMock.set(
                            key = any(),
                            value = any(),
                            expireSeconds = any(),
                        )
                    }
                }

                it("get and caches PdlPersonidentName") {
                    coEvery {
                        azureAdClientMock.getSystemToken(scopeClientId = pdlClientId)
                    } returns AzureAdTokenResponse(
                        access_token = "token",
                        expires_in = 3600,
                        token_type = "type",
                    ).toAzureAdToken()

                    every {
                        redisStoreMock.getObjectList(
                            classType = PdlPersonidentNameCache::class,
                            keyList = listOf(pdlPersonidentNameCacheKey),
                        )
                    } returns emptyList()

                    runBlocking {
                        client.personIdentNumberNavnMap(
                            callId = anyCallId,
                            personIdentNumberList = listOf(NARMESTELEDER_PERSONIDENTNUMBER)
                        ) shouldBeEqualTo mapOf(pdlHentPersonIdent.value to pdlHentPersonName)
                    }

                    verify(exactly = 1) {
                        redisStoreMock.getObjectList(
                            classType = PdlPersonidentNameCache::class,
                            keyList = listOf(pdlPersonidentNameCacheKey),
                        )
                    }
                    verify(exactly = 1) {
                        redisStoreMock.setObject(
                            key = pdlPersonidentNameCacheKey,
                            value = pdlPersonidentNameCache,
                            expireSeconds = CACHE_PDL_PERSONIDENT_NAME_TIME_TO_LIVE_SECONDS,
                        )
                    }
                }
            }
        }
    }
})
