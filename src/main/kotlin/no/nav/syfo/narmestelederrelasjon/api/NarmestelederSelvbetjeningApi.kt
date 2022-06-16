package no.nav.syfo.narmestelederrelasjon.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.getPersonIdentFromToken
import no.nav.syfo.narmestelederrelasjon.NarmesteLederRelasjonService
import no.nav.syfo.narmestelederrelasjon.domain.toNarmesteLederRelasjonDTO
import no.nav.syfo.util.*

const val narmesteLederSelvbetjeningApiV1Path = "/api/selvbetjening/v1/narmestelederrelasjoner"

fun Route.registrerNarmesteLederRelasjonSelvbetjeningApi(
    narmesteLederRelasjonService: NarmesteLederRelasjonService,
) {
    route(narmesteLederSelvbetjeningApiV1Path) {
        get {
            val callId = getCallId()
            val token = getBearerHeader()
                ?: throw IllegalArgumentException("No Authorization header supplied to selvbetjening api when getting narmestelederRelasjoner, callID=$callId")

            val personIdent = getPersonIdentFromToken(token)
                ?: throw IllegalArgumentException("No PersonIdent supplied to selvbetjening api when getting narmestelederRelasjoner, callID=$callId")

            val narmesteLederRelasjonDTOList = narmesteLederRelasjonService.getNarmestelederRelasjonList(
                callId = callId,
                personIdentNumber = personIdent,
            ).map {
                it.toNarmesteLederRelasjonDTO()
            }
            call.respond(narmesteLederRelasjonDTOList)
        }
    }
}
