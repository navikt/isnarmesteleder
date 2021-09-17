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
    val virksomhetsnummer: Virksomhetsnummer,
    val narmesteLederPersonIdentNumber: PersonIdentNumber,
    val narmesteLederTelefonnummer: String,
    val narmesteLederEpost: String,
    val aktivFom: LocalDate,
    val aktivTom: LocalDate?,
    val arbeidsgiverForskutterer: Boolean?,
    val timestamp: OffsetDateTime,
)

fun PNarmesteLederRelasjon.toNarmesteLederRelasjon(
    pNarmesteLederRelasjonList: List<PNarmesteLederRelasjon>,
) = NarmesteLederRelasjon(
    id = this.id,
    uuid = this.uuid,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    referanseUuid = this.referanseUuid,
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
        val narmesteLederRelasjonAktiv = pNarmesteLederRelasjonList.maxByOrNull { narmesteLederRelasjon ->
            narmesteLederRelasjon.timestamp
        } ?: throw Exception("Cannot find type of NarmesteLederRelasjon: Empty narmesteLederRelasjonList was supplied")

        val isThisAktiv = this.referanseUuid === narmesteLederRelasjonAktiv.referanseUuid
        if (isThisAktiv) {
            NarmesteLederRelasjonStatus.AKTIV
        } else {
            NarmesteLederRelasjonStatus.ERSTATTET
        }
    }
}
