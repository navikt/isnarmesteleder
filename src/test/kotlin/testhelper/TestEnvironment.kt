package testhelper

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment

fun testEnvironment(
    azureOpenIdTokenEndpoint: String = "azureTokenEndpoint",
) = Environment(
    azureAppClientId = "isdialogmote-client-id",
    azureAppSecret = "isdialogmote-secret",
    azureAppWellKnownUrl = "wellknown",
    azureOpenIdTokenEndpoint = azureOpenIdTokenEndpoint,
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)
