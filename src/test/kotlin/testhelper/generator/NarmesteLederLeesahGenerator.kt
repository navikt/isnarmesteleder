package testhelper.generator

import no.nav.syfo.narmestelederrelasjon.kafka.domain.NarmesteLederLeesah
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generateNarmesteLederLeesah() = NarmesteLederLeesah(
    narmesteLederId = UUID.randomUUID(),
    fnr = ARBEIDSTAKER_FNR.value,
    orgnummer = VIRKSOMHETSNUMMER_DEFAULT.value,
    narmesteLederFnr = NARMESTELEDER_PERSONIDENTNUMBER.value,
    narmesteLederTelefonnummer = "99119911",
    narmesteLederEpost = "test@test.com",
    aktivFom = LocalDate.now().minusDays(10),
    aktivTom = null,
    arbeidsgiverForskutterer = null,
    timestamp = OffsetDateTime.now().minusDays(5),
)
