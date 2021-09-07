package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import no.nav.syfo.metric.METRICS_REGISTRY
import no.nav.syfo.util.*
import java.util.*

fun Application.installMetrics() {
    install(MicrometerMetrics) {
        registry = METRICS_REGISTRY
    }
}

fun Application.installCallId() {
    install(CallId) {
        retrieve { it.request.headers[NAV_CALL_ID_HEADER] }
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(NAV_CALL_ID_HEADER)
    }
}

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        jackson(block = configureJacksonMapper())
    }
}

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            val callId = getCallId()
            val consumerId = getConsumerId()
            log.error("Caught exception, callId=$callId, consumerId=$consumerId", cause)
            throw cause
        }
    }
}
