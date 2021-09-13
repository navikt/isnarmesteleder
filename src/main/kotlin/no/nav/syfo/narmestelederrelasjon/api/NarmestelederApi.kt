package no.nav.syfo.narmestelederrelasjon.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.narmestelederrelasjon.NarmesteLederRelasjonService
import no.nav.syfo.narmestelederrelasjon.domain.toNarmesteLederRelasjonDTO
import no.nav.syfo.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val narmesteLederApiV1Path = "/api/v1/narmestelederrelasjon"
const val narmesteLederApiV1PersonIdentPath = "/personident"

fun Route.registrerNarmesteLederRelasjonApi(
    narmesteLederRelasjonService: NarmesteLederRelasjonService,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
) {
    route(narmesteLederApiV1Path) {
        get(narmesteLederApiV1PersonIdentPath) {
            val callId = getCallId()
            try {
                val token = getBearerHeader()
                    ?: throw IllegalArgumentException("No Authorization header supplied")

                val personIdentNumber = getPersonIdentHeader()?.let { personIdent ->
                    PersonIdentNumber(personIdent)
                } ?: throw IllegalArgumentException("No PersonIdent supplied")

                val hasAccessToPerson = veilederTilgangskontrollClient.hasAccess(
                    callId = callId,
                    personIdentNumber = personIdentNumber,
                    token = token,
                )
                if (hasAccessToPerson) {
                    val narmesteLederRelasjonDTOList = narmesteLederRelasjonService.getRelasjonerForPersonIdent(
                        personIdentNumber = personIdentNumber,
                    ).map {
                        it.toNarmesteLederRelasjonDTO()
                    }
                    call.respond(narmesteLederRelasjonDTOList)
                } else {
                    val accessDeniedMessage = "Denied Veileder access to PersonIdent with PersonIdent"
                    log.warn("$accessDeniedMessage, {}", callIdArgument(callId))
                    call.respond(HttpStatusCode.Forbidden, accessDeniedMessage)
                }
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Could not retrieve list of NarmesteLederRelasjon for PersonIdent"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callId)
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}
