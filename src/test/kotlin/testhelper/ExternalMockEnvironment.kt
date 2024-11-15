package testhelper

import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.cache.RedisStore
import testhelper.mock.*

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()

    val azureAdMock = AzureAdMock()
    val eregMock = EregMock()
    val pdlMock = PdlMock()
    val tilgangskontrollMock = VeilederTilgangskontrollMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdMock.name to azureAdMock.server,
        eregMock.name to eregMock.server,
        pdlMock.name to pdlMock.server,
        tilgangskontrollMock.name to tilgangskontrollMock.server,
    )

    val environment = testEnvironment(
        azureOpenIdTokenEndpoint = azureAdMock.url,
        eregUrl = eregMock.url,
        pdlUrl = pdlMock.url,
        tilgangskontrollUrl = tilgangskontrollMock.url,
    )
    lateinit var redisCache: RedisStore
    val redisServer = testRedis(environment.redisConfig)

    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val wellKnownSelvbetjening = wellKnownSelvbetjening()
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.externalApplicationMockMap.start()
    this.redisServer.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.externalApplicationMockMap.stop()
    this.database.stop()
    this.redisServer.stop()
}

fun HashMap<String, NettyApplicationEngine>.start() {
    this.forEach {
        it.value.start()
    }
}

fun HashMap<String, NettyApplicationEngine>.stop(
    gracePeriodMillis: Long = 1L,
    timeoutMillis: Long = 10L,
) {
    this.forEach {
        it.value.stop(gracePeriodMillis, timeoutMillis)
    }
}
