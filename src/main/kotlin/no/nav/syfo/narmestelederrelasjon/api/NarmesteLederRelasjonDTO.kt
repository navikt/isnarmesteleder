package no.nav.syfo.narmestelederrelasjon.api

import java.time.*

data class NarmesteLederRelasjonDTO(
    val uuid: String,
    val arbeidstakerPersonIdentNumber: String,
    val virksomhetsnummer: String,
    val narmesteLederPersonIdentNumber: String,
    val narmesteLederTelefonnummer: String,
    val narmesteLederEpost: String,
    val aktivFom: LocalDate,
    val aktivTom: LocalDate?,
    val arbeidsgiverForskutterer: Boolean?,
    val timestamp: LocalDateTime?,
)
