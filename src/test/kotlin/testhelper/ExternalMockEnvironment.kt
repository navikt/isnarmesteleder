package testhelper

import no.nav.syfo.application.ApplicationState
import testhelper.mock.*

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()

    val environment = testEnvironment()
    val mockHttpClient = mockHttpClient(environment = environment)
    val cache = InMemoryValkeyStore()

    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val wellKnownSelvbetjening = wellKnownSelvbetjening()

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
