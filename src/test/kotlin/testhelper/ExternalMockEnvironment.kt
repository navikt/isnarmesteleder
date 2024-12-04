package testhelper

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.cache.RedisStore
import testhelper.mock.*

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()

    val environment = testEnvironment()
    val mockHttpClient = mockHttpClient(environment = environment)

    lateinit var redisCache: RedisStore
    val redisServer = testRedis(environment.redisConfig)

    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val wellKnownSelvbetjening = wellKnownSelvbetjening()

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment().also { it.redisServer.start() }
    }
}
