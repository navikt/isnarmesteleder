package no.nav.syfo.util

import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import net.logstash.logback.argument.StructuredArguments
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

const val NAV_PERSONIDENT_HEADER = "nav-personident"
const val NAV_CALL_ID_HEADER = "Nav-Call-Id"

fun RoutingContext.getCallId(): String {
    return this.call.getCallId()
}

fun ApplicationCall.getCallId(): String {
    return this.request.headers[NAV_CALL_ID_HEADER].toString()
}

fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
fun ApplicationCall.getConsumerId(): String {
    return this.request.headers[NAV_CONSUMER_ID_HEADER].toString()
}

fun RoutingContext.getBearerHeader(): String? {
    return this.call.request.headers[Authorization]?.removePrefix("Bearer ")
}

fun RoutingContext.getPersonIdentHeader(): String? {
    return this.call.request.headers[NAV_PERSONIDENT_HEADER]
}

private val kafkaCounter = AtomicInteger(0)

fun kafkaCallId(): String = "${
LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-HHmm"))
}-isnarmesteleder-kafka-${kafkaCounter.incrementAndGet()}"
