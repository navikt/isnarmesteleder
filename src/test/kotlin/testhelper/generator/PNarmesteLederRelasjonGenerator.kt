package testhelper.generator

import no.nav.syfo.narmestelederrelasjon.database.domain.PNarmesteLederRelasjon
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generatePNarmesteLederRelasjon() = PNarmesteLederRelasjon(
    id = 1,
    uuid = UUID.randomUUID(),
    createdAt = OffsetDateTime.now(),
    updatedAt = OffsetDateTime.now(),
    referanseUuid = UUID.randomUUID(),
    arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR,
    virksomhetsnavn = null,
    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
    narmesteLederTelefonnummer = "99119911",
    narmesteLederEpost = "test@test.com",
    aktivFom = LocalDate.now().minusDays(10),
    aktivTom = null,
    arbeidsgiverForskutterer = null,
    timestamp = OffsetDateTime.now().minusDays(5),
)
