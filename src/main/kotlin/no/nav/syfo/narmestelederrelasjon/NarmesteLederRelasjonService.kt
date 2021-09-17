package no.nav.syfo.narmestelederrelasjon

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjon

class NarmesteLederRelasjonService(
    private val database: DatabaseInterface,
) {
    fun getRelasjonerForPersonIdent(
        personIdentNumber: PersonIdentNumber
    ): List<NarmesteLederRelasjon> {
        return database.getNarmesteLederRelasjonList(
            personIdentNumber = personIdentNumber,
        ).map {
            it.toNarmesteLederRelasjon()
        }
    }
}
