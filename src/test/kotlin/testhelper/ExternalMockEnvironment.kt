package testhelper

import no.nav.syfo.application.ApplicationState
import testhelper.mock.wellKnownInternalAzureAD

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val environment = testEnvironment()

    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.database.stop()
}
