package testhelper

import no.nav.syfo.application.*
import java.net.ServerSocket

fun testEnvironment(
    azureOpenIdTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBootstrapServers: String,
    syfotilgangskontrollUrl: String = "tilgangskontroll",
) = Environment(
    azureAppClientId = "isdialogmote-client-id",
    azureAppClientSecret = "isdialogmote-secret",
    azureAppWellKnownUrl = "wellknown",
    azureOpenidConfigTokenEndpoint = azureOpenIdTokenEndpoint,
    kafka = ApplicationEnvironmentKafka(
        aivenBootstrapServers = kafkaBootstrapServers,
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    isnarmestelederDbHost = "localhost",
    isnarmestelederDbPort = "5432",
    isnarmestelederDbName = "isnarmesteleder_dev",
    isnarmestelederDbUsername = "username",
    isnarmestelederDbPassword = "password",
    syfotilgangskontrollClientId = "dev-fss.teamsykefravr.syfo-tilgangskontroll",
    syfotilgangskontrollUrl = syfotilgangskontrollUrl,
    toggleKafkaProcessingEnabled = true,
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
