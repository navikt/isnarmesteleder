package no.nav.syfo.narmestelederrelasjon.database.domain

import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class PNarmesteLederRelasjon(
    val id: Int,
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val referanseUuid: UUID,
    val arbeidstakerPersonIdentNumber: PersonIdentNumber,
    val virksomhetsnavn: String?,
    val virksomhetsnummer: Virksomhetsnummer,
    val narmesteLederPersonIdentNumber: PersonIdentNumber,
    val narmesteLederTelefonnummer: String,
    val narmesteLederEpost: String,
    val aktivFom: LocalDate,
    val aktivTom: LocalDate?,
    val arbeidsgiverForskutterer: Boolean?,
    val timestamp: OffsetDateTime,
)

fun List<PNarmesteLederRelasjon>.toNarmesteLederRelasjonList(): List<NarmesteLederRelasjon> {
    val narmesteLederRelasjonVirksomhetsnummerMap = this.groupBy {
        it.virksomhetsnummer
    }
    val returnList = mutableListOf<NarmesteLederRelasjon>()

    narmesteLederRelasjonVirksomhetsnummerMap.entries.forEach { narmesteLederRelasjonVirksomhetsnummer ->
        val pNarmesteLederRelasjonListByVirksomhetsnummer = narmesteLederRelasjonVirksomhetsnummer.value
        returnList.addAll(
            pNarmesteLederRelasjonListByVirksomhetsnummer.map { pNarmesteLederRelasjon ->
                pNarmesteLederRelasjon.toNarmesteLederRelasjon(
                    pNarmesteLederRelasjonList = pNarmesteLederRelasjonListByVirksomhetsnummer
                )
            }.sortedByDescending {
                it.timestamp
            }
        )
    }
    return returnList
}

fun PNarmesteLederRelasjon.toNarmesteLederRelasjon(
    pNarmesteLederRelasjonList: List<PNarmesteLederRelasjon>,
) = NarmesteLederRelasjon(
    id = this.id,
    uuid = this.uuid,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    referanseUuid = this.referanseUuid,
    virksomhetsnavn = this.virksomhetsnavn,
    virksomhetsnummer = this.virksomhetsnummer,
    arbeidstakerPersonIdentNumber = this.arbeidstakerPersonIdentNumber,
    narmesteLederPersonIdentNumber = this.narmesteLederPersonIdentNumber,
    narmesteLederTelefonnummer = this.narmesteLederTelefonnummer,
    narmesteLederEpost = this.narmesteLederEpost,
    arbeidsgiverForskutterer = this.arbeidsgiverForskutterer,
    aktivFom = this.aktivFom,
    aktivTom = this.aktivTom,
    timestamp = this.timestamp,
    status = this.findStatus(pNarmesteLederRelasjonList = pNarmesteLederRelasjonList),
)

fun PNarmesteLederRelasjon.findStatus(
    pNarmesteLederRelasjonList: List<PNarmesteLederRelasjon>,
): NarmesteLederRelasjonStatus {
    val isNarmesteLederRelasjonDeaktivert = this.aktivTom != null
    return if (isNarmesteLederRelasjonDeaktivert) {
        NarmesteLederRelasjonStatus.DEAKTIVERT
    } else {
        val narmesteLederRelasjonNewest = pNarmesteLederRelasjonList.maxByOrNull { narmesteLederRelasjon ->
            narmesteLederRelasjon.timestamp
        } ?: throw Exception("Cannot find type of NarmesteLederRelasjon: Empty narmesteLederRelasjonList was supplied")

        val isThisAktiv = narmesteLederRelasjonNewest.aktivTom == null && this.referanseUuid === narmesteLederRelasjonNewest.referanseUuid
        if (isThisAktiv) {
            NarmesteLederRelasjonStatus.INNMELDT_AKTIV
        } else {
            NarmesteLederRelasjonStatus.INNMELDT_INAKTIV
        }
    }
}
