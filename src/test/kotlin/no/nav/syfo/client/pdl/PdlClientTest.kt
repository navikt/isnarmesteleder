package no.nav.syfo.client.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import kotlinx.coroutines.test.runTest
import no.nav.syfo.application.cache.ValkeyStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.azuread.AzureAdTokenResponse
import no.nav.syfo.client.azuread.toAzureAdToken
import no.nav.syfo.client.pdl.PdlClient.Companion.CACHE_PDL_PERSONIDENT_NAME_KEY_PREFIX
import no.nav.syfo.client.pdl.PdlClient.Companion.CACHE_PDL_PERSONIDENT_NAME_TIME_TO_LIVE_SECONDS
import no.nav.syfo.client.pdl.domain.fullName
import no.nav.syfo.util.configuredJacksonMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.ExternalMockEnvironment
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.mock.pdlPersonMockRespons

class PdlClientTest {

    private val objectMapper: ObjectMapper = configuredJacksonMapper()
    private val anyCallId = "callId"
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val azureAdClientMock = mockk<AzureAdClient>(relaxed = true)
    private val valkeyStoreMock = mockk<ValkeyStore>(relaxed = true)
    private val pdlClientId = "pdlClientId"

    private val client = PdlClient(
        azureAdClient = azureAdClientMock,
        clientEnvironment = externalMockEnvironment.environment.clients.pdl,
        valkeyStore = valkeyStoreMock,
        httpClient = externalMockEnvironment.mockHttpClient,
    )

    private val pdlHentPersonIdent = NARMESTELEDER_PERSONIDENTNUMBER
    private val pdlHentPersonName = pdlPersonMockRespons.data.hentPersonBolk?.first()?.person?.fullName() ?: ""
    private val pdlPersonidentNameCacheKey = "$CACHE_PDL_PERSONIDENT_NAME_KEY_PREFIX${pdlHentPersonIdent.value}"

    private val pdlPersonidentNameCache = PdlPersonidentNameCache(
        name = pdlHentPersonName,
        personIdent = pdlHentPersonIdent.value,
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(valkeyStoreMock)
    }

    @Test
    fun `returns cached PdlPersonidentName`() = runTest {
        every {
            valkeyStoreMock.get(keyList = listOf(pdlPersonidentNameCacheKey))
        } returns listOf(objectMapper.writeValueAsString(pdlPersonidentNameCache))

        assertEquals(
            mapOf(pdlHentPersonIdent.value to pdlHentPersonName),
            client.personIdentNumberNavnMap(
                callId = anyCallId,
                personIdentNumberList = listOf(NARMESTELEDER_PERSONIDENTNUMBER)
            )
        )
        verify(exactly = 1) {
            valkeyStoreMock.getObjectList(
                classType = PdlPersonidentNameCache::class,
                keyList = listOf(pdlPersonidentNameCacheKey),
            )
        }
        verify(exactly = 0) {
            valkeyStoreMock.set(
                key = any(),
                value = any(),
                expireSeconds = any(),
            )
        }
    }

    @Test
    fun `get and caches PdlPersonidentName`() = runTest {
        coEvery {
            azureAdClientMock.getSystemToken(scopeClientId = pdlClientId)
        } returns AzureAdTokenResponse(
            access_token = "token",
            expires_in = 3600,
            token_type = "type",
        ).toAzureAdToken()

        every {
            valkeyStoreMock.getObjectList(
                classType = PdlPersonidentNameCache::class,
                keyList = listOf(pdlPersonidentNameCacheKey),
            )
        } returns emptyList()

        assertEquals(
            mapOf(pdlHentPersonIdent.value to pdlHentPersonName),
            client.personIdentNumberNavnMap(
                callId = anyCallId,
                personIdentNumberList = listOf(NARMESTELEDER_PERSONIDENTNUMBER)
            )
        )

        verify(exactly = 1) {
            valkeyStoreMock.getObjectList(
                classType = PdlPersonidentNameCache::class,
                keyList = listOf(pdlPersonidentNameCacheKey),
            )
        }
        verify(exactly = 1) {
            valkeyStoreMock.setObject(
                key = pdlPersonidentNameCacheKey,
                value = pdlPersonidentNameCache,
                expireSeconds = CACHE_PDL_PERSONIDENT_NAME_TIME_TO_LIVE_SECONDS,
            )
        }
    }
}
