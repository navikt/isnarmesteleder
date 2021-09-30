package no.nav.syfo.narmestelederrelasjon

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.domain.addNarmesteLederName

class NarmesteLederRelasjonService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {
    suspend fun getRelasjonerForPersonIdent(
        callId: String,
        personIdentNumber: PersonIdentNumber,
    ): List<NarmesteLederRelasjon> {
        val narmesteLederRelasjonList = database.getNarmesteLederRelasjonList(
            personIdentNumber = personIdentNumber,
        ).toNarmesteLederRelasjonList()

        val narmesteLederPersonIdentNumberList = narmesteLederRelasjonList.map { narmesteLederRelasjon ->
            narmesteLederRelasjon.narmesteLederPersonIdentNumber
        }
        pdlClient.personIdentNumberNavnMap(
            callId = callId,
            personIdentNumberList = narmesteLederPersonIdentNumberList,
        ).let { maybePersonIdentNumberNameMap ->
            return narmesteLederRelasjonList.addNarmesteLederName(
                maybePersonIdentNumberNameMap = maybePersonIdentNumberNameMap,
            )
        }
    }
}
