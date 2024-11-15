package testhelper

import no.nav.syfo.application.cache.RedisConfig
import redis.embedded.RedisServer

fun testRedis(
    redisEnvironment: RedisConfig,
): RedisServer = RedisServer.builder()
    .port(redisEnvironment.port)
    .setting("requirepass ${redisEnvironment.redisPassword}")
    .build()
