package no.nav.syfo.narmestelederrelasjon.api

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.narmestelederrelasjon.NarmesteLederRelasjonService
import no.nav.syfo.narmestelederrelasjon.api.access.APIConsumerAccessService
import no.nav.syfo.narmestelederrelasjon.domain.toNarmesteLederRelasjonDTO
import no.nav.syfo.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val narmesteLederSystemApiV1Path = "/api/system/v1/narmestelederrelasjoner"

fun Route.registrerNarmesteLederRelasjonSystemApi(
    apiConsumerAccessService: APIConsumerAccessService,
    authorizedApplicationNameList: List<String>,
    narmesteLederRelasjonService: NarmesteLederRelasjonService,
) {
    route(narmesteLederSystemApiV1Path) {
        get {
            val callId = getCallId()
            val token = getBearerHeader()
                ?: throw IllegalArgumentException("No Authorization header supplied to system api when getting narmestelederRelasjoner, callID=$callId")

            apiConsumerAccessService.validateConsumerApplicationAZP(
                authorizedApplicationNameList = authorizedApplicationNameList,
                token = token,
            )

            val personIdentNumber = getPersonIdentHeader()?.let { personIdent ->
                PersonIdentNumber(personIdent)
            }
                ?: throw IllegalArgumentException("No PersonIdent supplied to system api when getting narmestelederRelasjoner, callID=$callId")

            val narmesteLederRelasjonDTOList = narmesteLederRelasjonService.getNarmestelederRelasjonList(
                callId = callId,
                arbeidstakerPersonIdentNumber = personIdentNumber,
            ).map {
                it.toNarmesteLederRelasjonDTO()
            }
            call.respond(narmesteLederRelasjonDTOList)
        }
    }
}
