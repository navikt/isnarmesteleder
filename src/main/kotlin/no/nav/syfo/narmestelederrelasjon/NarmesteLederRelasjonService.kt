package no.nav.syfo.narmestelederrelasjon

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.narmestelederrelasjon.database.domain.PNarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLedere
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.domain.addNarmesteLederName

class NarmesteLederRelasjonService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {
    suspend fun getNarmesteLedere(
        callId: String,
        arbeidstakerPersonIdentNumber: PersonIdentNumber,
    ): List<NarmesteLederRelasjon> {
        val narmesteLederRelasjonHistoryList = getNarmesteLedereHistory(
            callId = callId,
            arbeidstakerPersonIdentNumber = arbeidstakerPersonIdentNumber,
        )

        return if (narmesteLederRelasjonHistoryList.isEmpty()) {
            narmesteLederRelasjonHistoryList
        } else {
            getNarmesteLederRelasjonListWithName(
                callId = callId,
                narmesteLederRelasjonList = narmesteLederRelasjonHistoryList,
            ).map { narmesteLederRelasjon ->
                narmesteLederRelasjon.copy(
                    arbeidstakerPersonIdentNumber = arbeidstakerPersonIdentNumber,
                )
            }
        }
    }

    suspend fun getNarmestelederRelasjonList(
        callId: String,
        personIdentNumber: PersonIdentNumber,
    ): List<NarmesteLederRelasjon> {
        val narmesteLederRelasjonHistoryList = getNarmesteLederRelasjonHistoryList(
            callId = callId,
            personIdentNumber = personIdentNumber,
        )

        return if (narmesteLederRelasjonHistoryList.isEmpty()) {
            narmesteLederRelasjonHistoryList
        } else {
            getNarmesteLederRelasjonListWithName(
                callId = callId,
                narmesteLederRelasjonList = narmesteLederRelasjonHistoryList,
            )
        }
    }

    private suspend fun getNarmesteLederRelasjonHistoryList(
        callId: String,
        personIdentNumber: PersonIdentNumber,
    ): List<NarmesteLederRelasjon> =
        pdlClient.identList(
            callId = callId,
            withHistory = true,
            personIdentNumber = personIdentNumber,
        )?.flatMap { personIdent ->
            val liste = database.getNarmesteLederRelasjonList(
                personIdentNumber = personIdent,
            )
            liste.map { narmestelederRelasjon ->
                narmestelederRelasjon.replaceIdent(
                    oldIdent = personIdent,
                    newIdent = personIdentNumber,
                )
            }
        }?.toNarmesteLederRelasjonList()
            ?: emptyList()

    private fun PNarmesteLederRelasjon.replaceIdent(
        oldIdent: PersonIdentNumber,
        newIdent: PersonIdentNumber
    ): PNarmesteLederRelasjon {
        return this.copy(
            arbeidstakerPersonIdentNumber = if (this.arbeidstakerPersonIdentNumber == oldIdent) newIdent else this.arbeidstakerPersonIdentNumber,
            narmesteLederPersonIdentNumber = if (this.narmesteLederPersonIdentNumber == oldIdent) newIdent else this.narmesteLederPersonIdentNumber
        )
    }

    private suspend fun getNarmesteLederRelasjonListWithName(
        callId: String,
        narmesteLederRelasjonList: List<NarmesteLederRelasjon>,
    ): List<NarmesteLederRelasjon> {
        val narmesteLederPersonIdentNumberList = narmesteLederRelasjonList.map { narmesteLederRelasjon ->
            narmesteLederRelasjon.narmesteLederPersonIdentNumber
        }
        pdlClient.personIdentNumberNavnMap(
            callId = callId,
            personIdentNumberList = narmesteLederPersonIdentNumberList,
        ).let { personIdentNumberNameMap ->
            return narmesteLederRelasjonList.addNarmesteLederName(
                maybePersonIdentNumberNameMap = personIdentNumberNameMap,
            )
        }
    }

    private suspend fun getNarmesteLedereHistory(
        callId: String,
        arbeidstakerPersonIdentNumber: PersonIdentNumber,
    ): List<NarmesteLederRelasjon> =
        pdlClient.identList(
            callId = callId,
            withHistory = true,
            personIdentNumber = arbeidstakerPersonIdentNumber,
        )?.flatMap { personIdent ->
            database.getNarmesteLedere(
                personIdentNumber = personIdent,
            )
        }?.toNarmesteLederRelasjonList()
            ?: emptyList()
}
