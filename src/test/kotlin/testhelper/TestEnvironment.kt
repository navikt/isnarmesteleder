package testhelper

import no.nav.syfo.application.*
import no.nav.syfo.narmestelederrelasjon.api.access.PreAuthorizedClient
import no.nav.syfo.util.configuredJacksonMapper
import java.net.ServerSocket

fun testEnvironment(
    azureOpenIdTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBootstrapServers: String,
    isproxyUrl: String = "isproxy",
    pdlUrl: String = "pdl",
    syfotilgangskontrollUrl: String = "tilgangskontroll",
) = Environment(
    azureAppClientId = "isnarmesteleder-client-id",
    azureAppClientSecret = "isnarmesteleder-secret",
    azureAppPreAuthorizedApps = configuredJacksonMapper().writeValueAsString(testAzureAppPreAuthorizedApps),
    azureAppWellKnownUrl = "wellknown",
    azureOpenidConfigTokenEndpoint = azureOpenIdTokenEndpoint,
    electorPath = "electorPath",
    kafka = ApplicationEnvironmentKafka(
        aivenBootstrapServers = kafkaBootstrapServers,
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    redisHost = "localhost",
    redisSecret = "password",
    isnarmestelederDbHost = "localhost",
    isnarmestelederDbPort = "5432",
    isnarmestelederDbName = "isnarmesteleder_dev",
    isnarmestelederDbUsername = "username",
    isnarmestelederDbPassword = "password",
    isproxyClientId = "dev-fss.teamsykefravr.isproxy",
    isproxyUrl = isproxyUrl,
    pdlClientId = "dev-fss.pdl.pdl-api",
    pdlUrl = pdlUrl,
    syfotilgangskontrollClientId = "dev-fss.teamsykefravr.syfo-tilgangskontroll",
    syfotilgangskontrollUrl = syfotilgangskontrollUrl,
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}

const val testSyfomoteadminClientId = "syfomoteadmin-client-id"
val testAzureAppPreAuthorizedApps = listOf(
    PreAuthorizedClient(
        name = "dev-fss:teamsykefravr:syfomoteadmin",
        clientId = testSyfomoteadminClientId,
    ),
)
