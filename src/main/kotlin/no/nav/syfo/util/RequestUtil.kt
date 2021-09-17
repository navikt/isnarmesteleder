package no.nav.syfo.util

import io.ktor.application.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.util.pipeline.*
import net.logstash.logback.argument.StructuredArguments
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

const val NAV_PERSONIDENT_HEADER = "nav-personident"

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String {
    return this.call.request.headers[NAV_CALL_ID_HEADER].toString()
}
fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
fun PipelineContext<out Unit, ApplicationCall>.getConsumerId(): String {
    return this.call.request.headers[NAV_CONSUMER_ID_HEADER].toString()
}

fun PipelineContext<out Unit, ApplicationCall>.getBearerHeader(): String? {
    return this.call.request.headers[Authorization]?.removePrefix("Bearer ")
}

fun PipelineContext<out Unit, ApplicationCall>.getPersonIdentHeader(): String? {
    return this.call.request.headers[NAV_PERSONIDENT_HEADER]
}

private val kafkaCounter = AtomicInteger(0)

fun kafkaCallId(): String = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-HHmm"))}-isnarmesteleder-kafka-${kafkaCounter.incrementAndGet()}"
