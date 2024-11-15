package testhelper

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.cache.RedisStore
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

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        wellKnownSelvbetjening = externalMockEnvironment.wellKnownSelvbetjening,
        redisStore = cache,
    )
}
