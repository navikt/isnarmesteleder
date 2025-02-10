package testhelper

import no.nav.syfo.application.cache.ValkeyConfig
import redis.embedded.RedisServer

fun testRedis(
    valkeyEnvironment: ValkeyConfig,
): RedisServer = RedisServer.builder()
    .port(valkeyEnvironment.port)
    .setting("requirepass ${valkeyEnvironment.valkeyPassword}")
    .build()
