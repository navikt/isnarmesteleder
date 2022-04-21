package testhelper

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.cache.RedisEnvironment
import no.nav.syfo.application.database.DatabaseEnvironment
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.ClientsEnvironment
import no.nav.syfo.client.azuread.AzureEnvironment
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
    azure = AzureEnvironment(
        appClientId = "appClientId",
        appClientSecret = "appClientSecret",
        appPreAuthorizedApps = configuredJacksonMapper().writeValueAsString(testAzureAppPreAuthorizedApps),
        appWellKnownUrl = "appWellKnownUrl",
        openidConfigTokenEndpoint = azureOpenIdTokenEndpoint,
    ),
    database = DatabaseEnvironment(
        host = "localhost",
        port = "5432",
        name = "isnarmesteleder_dev",
        username = "username",
        password = "password",
    ),
    electorPath = "electorPath",
    kafka = KafkaEnvironment(
        aivenBootstrapServers = kafkaBootstrapServers,
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    clients = ClientsEnvironment(
        isproxy = ClientEnvironment(
            baseUrl = isproxyUrl,
            clientId = "dev-fss.teamsykefravr.isproxy",
        ),
        pdl = ClientEnvironment(
            baseUrl = pdlUrl,
            clientId = "dev-fss.pdl.pdl-api",
        ),
        syfotilgangskontroll = ClientEnvironment(
            baseUrl = syfotilgangskontrollUrl,
            clientId = "dev-fss.teamsykefravr.syfotilgangskontroll",
        ),
    ),
    redis = RedisEnvironment(
        host = "localhost",
        port = 6379,
        secret = "password",
    ),
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
