package no.nav.syfo.narmestelederrelasjon.domain

import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.narmestelederrelasjon.api.NarmesteLederRelasjonDTO
import no.nav.syfo.util.toLocalDateTimeOslo
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class NarmesteLederRelasjon(
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
    val narmesteLederNavn: String? = null,
    val aktivFom: LocalDate,
    val aktivTom: LocalDate?,
    val arbeidsgiverForskutterer: Boolean?,
    val timestamp: OffsetDateTime,
    val status: NarmesteLederRelasjonStatus,
)

fun NarmesteLederRelasjon.toNarmesteLederRelasjonDTO() = NarmesteLederRelasjonDTO(
    uuid = this.uuid.toString(),
    virksomhetsnavn = this.virksomhetsnavn,
    virksomhetsnummer = this.virksomhetsnummer.value,
    arbeidstakerPersonIdentNumber = this.arbeidstakerPersonIdentNumber.value,
    narmesteLederPersonIdentNumber = this.narmesteLederPersonIdentNumber.value,
    narmesteLederTelefonnummer = this.narmesteLederTelefonnummer,
    narmesteLederEpost = this.narmesteLederEpost,
    narmesteLederNavn = this.narmesteLederNavn,
    arbeidsgiverForskutterer = this.arbeidsgiverForskutterer,
    aktivFom = this.aktivFom,
    aktivTom = this.aktivTom,
    timestamp = this.timestamp.toLocalDateTimeOslo(),
    status = this.status.name,
)

fun List<NarmesteLederRelasjon>.addNarmesteLederName(
    maybePersonIdentNumberNameMap: Map<String, String>?,
): List<NarmesteLederRelasjon> {
    if (maybePersonIdentNumberNameMap == null) {
        return this
    }
    return this.map { narmesteLederRelasjon ->
        val narmesteLederPersonIdentNumber = narmesteLederRelasjon.narmesteLederPersonIdentNumber.value
        narmesteLederRelasjon.copy(
            narmesteLederNavn = maybePersonIdentNumberNameMap[narmesteLederPersonIdentNumber]
        )
    }
}

enum class NarmesteLederRelasjonStatus {
    INNMELDT_AKTIV,
    DEAKTIVERT,
}
