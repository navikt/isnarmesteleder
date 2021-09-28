package no.nav.syfo.cronjob.virksomhetsnavn

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLederRelasjonWithoutVirksomhetsnavn
import no.nav.syfo.narmestelederrelasjon.database.updateNarmesteLederRelasjonVirksomhetsnavn
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjon

class VirksomhetsnavnService(
    private val database: DatabaseInterface
) {
    fun getNarmesteLederRelasjonWithoutVirksomhetsnavnList(): List<NarmesteLederRelasjon> {
        val pNarmesteLederRelasjonList = database.getNarmesteLederRelasjonWithoutVirksomhetsnavn()
        return pNarmesteLederRelasjonList.toNarmesteLederRelasjonList()
    }

    fun updateVirksomhetsnavn(
        narmesteLederRelasjonId: Int,
        virksomhetsnavn: String,
    ) {
        database.updateNarmesteLederRelasjonVirksomhetsnavn(
            narmesteLederRelasjonId = narmesteLederRelasjonId,
            virksomhetsnavn = virksomhetsnavn,
        )
    }
}
