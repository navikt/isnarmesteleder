package testhelper

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.narmestelederrelasjon.NarmesteLederRelasjonService
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val redisConfig = externalMockEnvironment.environment.redisConfig
    val cache = RedisStore(
        JedisPool(
            JedisPoolConfig(),
            HostAndPort(redisConfig.host, redisConfig.port),
            DefaultJedisClientConfig.builder()
                .ssl(redisConfig.ssl)
                .password(redisConfig.redisPassword)
                .build()
        )
    )
    externalMockEnvironment.redisCache = cache

    val azureAdClient = AzureAdClient(
        azureEnviroment = externalMockEnvironment.environment.azure,
        redisStore = cache,
        httpClient = externalMockEnvironment.mockHttpClient,
    )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        wellKnownSelvbetjening = externalMockEnvironment.wellKnownSelvbetjening,
        veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
            azureAdClient = azureAdClient,
            clientEnvironment = externalMockEnvironment.environment.clients.tilgangskontroll,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
        narmesteLederRelasjonService = NarmesteLederRelasjonService(
            database = externalMockEnvironment.database,
            pdlClient = PdlClient(
                azureAdClient = azureAdClient,
                clientEnvironment = externalMockEnvironment.environment.clients.pdl,
                redisStore = cache,
                httpClient = externalMockEnvironment.mockHttpClient,
            ),
        )
    )
}
