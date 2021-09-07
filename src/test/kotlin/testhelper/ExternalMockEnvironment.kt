package testhelper

import no.nav.syfo.application.ApplicationState
import testhelper.mock.wellKnownInternalAzureAD

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()

    val environment = testEnvironment()

    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
}
