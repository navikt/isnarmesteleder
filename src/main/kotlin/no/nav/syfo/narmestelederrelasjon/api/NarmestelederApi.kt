package no.nav.syfo.narmestelederrelasjon.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getPersonIdentHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val narmesteLederApiV1Path = "/api/v1/narmestelederrelasjon"
const val narmesteLederApiV1PersonIdentPath = "/personident"

fun Route.registrerNarmesteLederRelasjonApi() {
    route(narmesteLederApiV1Path) {
        get(narmesteLederApiV1PersonIdentPath) {
            val callId = getCallId()
            try {
                val personIdentNumber = getPersonIdentHeader()?.let { personIdent ->
                    PersonIdentNumber(personIdent)
                } ?: throw IllegalArgumentException("No PersonIdent supplied")

                call.respond(emptyList<NarmesteLederRelasjonDTO>())
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Could not retrieve list of NarmesteLeder for PersonIdent"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callId)
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}
