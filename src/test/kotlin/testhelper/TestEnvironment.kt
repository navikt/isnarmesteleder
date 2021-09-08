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
    isnarmestelederDbHost = "localhost",
    isnarmestelederDbPort = "5432",
    isnarmestelederDbName = "isnarmesteleder_dev",
    isnarmestelederDbUsername = "username",
    isnarmestelederDbPassword = "password",
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)
