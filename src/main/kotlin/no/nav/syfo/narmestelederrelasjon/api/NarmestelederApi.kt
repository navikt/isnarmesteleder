package no.nav.syfo.narmestelederrelasjon.api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
            val token = getBearerHeader()
                ?: throw IllegalArgumentException("Could not retrieve list of NarmesteLederRelasjon for PersonIdent: No Authorization header supplied")

            val personIdentNumber = getPersonIdentHeader()?.let { personIdent ->
                PersonIdentNumber(personIdent)
            }
                ?: throw IllegalArgumentException("Could not retrieve list of NarmesteLederRelasjon for PersonIdent: No PersonIdent supplied")

            val hasAccessToPerson = veilederTilgangskontrollClient.hasAccess(
                callId = callId,
                personIdentNumber = personIdentNumber,
                token = token,
            )
            if (hasAccessToPerson) {
                val narmesteLederRelasjonDTOList = narmesteLederRelasjonService.getNarmesteLedere(
                    callId = callId,
                    arbeidstakerPersonIdentNumber = personIdentNumber,
                ).map {
                    it.toNarmesteLederRelasjonDTO()
                }
                call.respond(narmesteLederRelasjonDTOList)
            } else {
                val accessDeniedMessage = "Denied Veileder access to PersonIdent with PersonIdent"
                log.warn("$accessDeniedMessage, {}", callIdArgument(callId))
                call.respond(HttpStatusCode.Forbidden, accessDeniedMessage)
            }
        }
    }
}
