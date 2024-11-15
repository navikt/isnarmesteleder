package testhelper

import no.nav.syfo.application.*
import no.nav.syfo.application.cache.RedisConfig
import no.nav.syfo.application.database.DatabaseEnvironment
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.ClientsEnvironment
import no.nav.syfo.client.azuread.AzureEnvironment
import no.nav.syfo.narmestelederrelasjon.api.access.PreAuthorizedClient
import no.nav.syfo.util.configuredJacksonMapper
import java.net.ServerSocket
import java.net.URI

fun testEnvironment(
    azureOpenIdTokenEndpoint: String = "azureTokenEndpoint",
    eregUrl: String = "ereg",
    pdlUrl: String = "pdl",
    tilgangskontrollUrl: String = "tilgangskontroll",
) = Environment(
    azure = AzureEnvironment(
        appClientId = "appClientId",
        appClientSecret = "appClientSecret",
        appPreAuthorizedApps = configuredJacksonMapper().writeValueAsString(testAzureAppPreAuthorizedApps),
        appWellKnownUrl = "appWellKnownUrl",
        openidConfigTokenEndpoint = azureOpenIdTokenEndpoint,
    ),
    tokenx = TokenxEnvironment(
        tokenxClientId = "tokenxClientId",
        tokenxWellKnownUrl = "tokenxWellKnownUrl",
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
        aivenBootstrapServers = "kafkaBootstrapServers",
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    clients = ClientsEnvironment(
        ereg = ClientEnvironment(
            baseUrl = eregUrl,
            clientId = "",
        ),
        pdl = ClientEnvironment(
            baseUrl = pdlUrl,
            clientId = "dev-fss.pdl.pdl-api",
        ),
        tilgangskontroll = ClientEnvironment(
            baseUrl = tilgangskontrollUrl,
            clientId = "dev-gcp.teamsykefravr.istilgangskontroll",
        ),
    ),
    redisConfig = RedisConfig(
        redisUri = URI("http://localhost:6379"),
        redisDB = 0,
        redisUsername = "redisUser",
        redisPassword = "redisPassword",
        ssl = false,
    ),
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}

const val testIsdialogmoteClientId = "isdialogmote-client-id"
const val testSyfomodiapersonClientId = "syfomodiaperson-client-id"
const val testSyfomotebehovClientId = "syfomotebehov-client-id"

val testAzureAppPreAuthorizedApps = listOf(
    PreAuthorizedClient(
        name = "dev-gcp:teamsykefravr:isdialogmote",
        clientId = testIsdialogmoteClientId,
    ),
    PreAuthorizedClient(
        name = "dev-fss:teamsykefravr:syfomodiaperson",
        clientId = testSyfomodiapersonClientId,
    ),
    PreAuthorizedClient(
        name = "dev-fss:team-esyfo:syfomotebehov",
        clientId = testSyfomotebehovClientId,
    ),
)
