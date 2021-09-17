package no.nav.syfo.narmestelederrelasjon

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjon

class NarmesteLederRelasjonService(
    private val database: DatabaseInterface,
) {
    fun getRelasjonerForPersonIdent(
        personIdentNumber: PersonIdentNumber
    ): List<NarmesteLederRelasjon> {
        val pNarmesteLederRelasjonList = database.getNarmesteLederRelasjonList(
            personIdentNumber = personIdentNumber,
        )
        return pNarmesteLederRelasjonList.toNarmesteLederRelasjonList()
    }
}
