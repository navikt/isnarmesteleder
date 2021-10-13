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
    val status: String?,
)

fun List<PNarmesteLederRelasjon>.toNarmesteLederRelasjonList(): List<NarmesteLederRelasjon> {
    val returnList = mutableListOf<NarmesteLederRelasjon>()

    val narmesteLederRelasjonReferanseMap = this.groupBy {
        it.referanseUuid
    }
    narmesteLederRelasjonReferanseMap.entries.map { narmesteLederRelasjonReferanseEntry ->
        val newestnarmesteLederRelasjonForReferanse =
            narmesteLederRelasjonReferanseEntry.value.maxByOrNull { pNarmesteLederRelasjon ->
                pNarmesteLederRelasjon.timestamp
            }
        newestnarmesteLederRelasjonForReferanse
            ?.toNarmesteLederRelasjon()
            ?.let { narmesteLederRelasjon ->
                returnList.add(narmesteLederRelasjon)
            }
    }
    return returnList.sortedByDescending {
        it.timestamp
    }
}

fun PNarmesteLederRelasjon.toNarmesteLederRelasjon() = NarmesteLederRelasjon(
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
    status = this.findStatus(),
)

fun PNarmesteLederRelasjon.findStatus(): NarmesteLederRelasjonStatus {
    val isNarmesteLederRelasjonDeaktivert = this.aktivTom != null
    return if (isNarmesteLederRelasjonDeaktivert) {
        NarmesteLederRelasjonStatus.DEAKTIVERT
    } else {
        NarmesteLederRelasjonStatus.INNMELDT_AKTIV
    }
}
