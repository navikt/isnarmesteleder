package no.nav.syfo.narmestelederrelasjon

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.narmestelederrelasjon.database.domain.PNarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.database.getNarmesteLedere
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.domain.addNarmesteLederName
import no.nav.syfo.narmestelederrelasjon.domain.addVirksomhetsnavn

class NarmesteLederRelasjonService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
    private val eregClient: EregClient,
) {
    suspend fun getNarmesteLedere(
        callId: String,
        arbeidstakerPersonIdentNumber: PersonIdentNumber,
    ): List<NarmesteLederRelasjon> {
        return getNarmesteLedereHistory(
            callId = callId,
            arbeidstakerPersonIdentNumber = arbeidstakerPersonIdentNumber,
        )
            .enrichRelationsWithNames(callId = callId)
            .map { narmesteLederRelasjon ->
                narmesteLederRelasjon.copy(
                    arbeidstakerPersonIdentNumber = arbeidstakerPersonIdentNumber,
                )
            }
    }

    suspend fun getNarmesteLederRelasjonList(
        callId: String,
        personIdentNumber: PersonIdentNumber,
    ): List<NarmesteLederRelasjon> {
        return getNarmesteLederRelasjonHistoryList(
            callId = callId,
            personIdentNumber = personIdentNumber,
        )
            .enrichRelationsWithNames(callId = callId)
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

    private suspend fun List<NarmesteLederRelasjon>.enrichRelationsWithNames(
        callId: String,
    ): List<NarmesteLederRelasjon> =
        if (isEmpty()) {
            this
        } else {
            coroutineScope {
                val virksomhetsnavnMapDeferred = async { virksomhetsnavnMapFromEreg(callId = callId) }
                val narmesteLederNavnMapDeferred = async { narmesteLederNavnMapFromPdl(callId = callId) }

                addVirksomhetsnavn(
                    maybeVirksomhetsnavnMap = virksomhetsnavnMapDeferred.await(),
                ).addNarmesteLederName(
                    maybePersonIdentNumberNameMap = narmesteLederNavnMapDeferred.await(),
                )
            }
        }

    private suspend fun List<NarmesteLederRelasjon>.narmesteLederNavnMapFromPdl(
        callId: String,
    ): Map<String, String> =
        if (isEmpty()) {
            emptyMap()
        } else {
            val narmesteLederPersonIdentNumberList = map { narmesteLederRelasjon ->
                narmesteLederRelasjon.narmesteLederPersonIdentNumber
            }.distinct()
            pdlClient.personIdentNumberNavnMap(
                callId = callId,
                personIdentNumberList = narmesteLederPersonIdentNumberList,
            )
        }

    private suspend fun List<NarmesteLederRelasjon>.virksomhetsnavnMapFromEreg(
        callId: String,
    ): Map<Virksomhetsnummer, String> =
        if (isEmpty()) {
            emptyMap()
        } else {
            coroutineScope {
                map { narmesteLederRelasjon ->
                    narmesteLederRelasjon.virksomhetsnummer
                }.distinct().map { virksomhetsnummer ->
                    async {
                        eregClient.organisasjonVirksomhetsnavn(
                            callId = callId,
                            virksomhetsnummer = virksomhetsnummer,
                        )
                            ?.let { eregOrganisasjonVirksomhetsnavn -> virksomhetsnummer to eregOrganisasjonVirksomhetsnavn.virksomhetsnavn }
                    }
                }.awaitAll().filterNotNull().toMap()
            }
        }
}
