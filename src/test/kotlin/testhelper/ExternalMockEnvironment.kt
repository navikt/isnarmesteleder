package testhelper

import io.ktor.server.netty.*
import no.nav.common.KafkaEnvironment
import no.nav.syfo.application.ApplicationState
import testhelper.mock.*

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val embeddedEnvironment: KafkaEnvironment = testKafka()

    val azureAdMock = AzureAdMock()
    val isproxyMock = IsproxyMock()
    val pdlMock = PdlMock()
    val tilgangskontrollMock = VeilederTilgangskontrollMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdMock.name to azureAdMock.server,
        isproxyMock.name to isproxyMock.server,
        pdlMock.name to pdlMock.server,
        tilgangskontrollMock.name to tilgangskontrollMock.server,
    )

    val environment = testEnvironment(
        azureOpenIdTokenEndpoint = azureAdMock.url,
        kafkaBootstrapServers = embeddedEnvironment.brokersURL,
        isproxyUrl = isproxyMock.url,
        pdlUrl = pdlMock.url,
        syfotilgangskontrollUrl = tilgangskontrollMock.url,
    )
    val redisServer = testRedis(
        port = environment.redisPort,
        secret = environment.redisSecret,
    )

    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.embeddedEnvironment.start()
    this.externalApplicationMockMap.start()
    this.redisServer.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.externalApplicationMockMap.stop()
    this.database.stop()
    this.embeddedEnvironment.tearDown()
    this.redisServer.stop()
}

fun HashMap<String, NettyApplicationEngine>.start() {
    this.forEach {
        it.value.start()
    }
}

fun HashMap<String, NettyApplicationEngine>.stop(
    gracePeriodMillis: Long = 1L,
    timeoutMillis: Long = 10L,
) {
    this.forEach {
        it.value.stop(gracePeriodMillis, timeoutMillis)
    }
}
