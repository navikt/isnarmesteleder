package no.nav.syfo.narmestelederrelasjon

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.domain.*

class NarmesteLederRelasjonService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {
    suspend fun getNarmestelederRelasjonList(
        callId: String,
        personIdentNumber: PersonIdentNumber,
        shouldGetAnsatte: Boolean,
    ): List<NarmesteLederRelasjon> {
        val narmesteLederRelasjonHistoryList = getNarmesteLederRelasjonHistoryList(
            callId = callId,
            personIdentNumber = personIdentNumber,
            shouldGetAnsatte = shouldGetAnsatte,
        )

        return if (narmesteLederRelasjonHistoryList.isEmpty()) {
            narmesteLederRelasjonHistoryList
        } else {
            getNarmesteLederRelasjonListWithName(
                callId = callId,
                narmesteLederRelasjonList = narmesteLederRelasjonHistoryList,
            ).map { narmesteLederRelasjon ->
                if (shouldGetAnsatte) {
                    narmesteLederRelasjon.newNarmesteLederPersonIdentNumber(personIdentNumber)
                } else {
                    narmesteLederRelasjon.newArbeidstakerPersonIdentNumber(personIdentNumber)
                }
            }
        }
    }

    private suspend fun getNarmesteLederRelasjonHistoryList(
        callId: String,
        personIdentNumber: PersonIdentNumber,
        shouldGetAnsatte: Boolean,
    ): List<NarmesteLederRelasjon> =
        pdlClient.identList(
            callId = callId,
            withHistory = true,
            personIdentNumber = personIdentNumber,
        )?.flatMap { personIdent ->
            database.getNarmesteLederRelasjonList(
                personIdentNumber = personIdent,
                shouldGetAnsatte = shouldGetAnsatte,
            )
        }?.toNarmesteLederRelasjonList()
            ?: emptyList()

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
}
