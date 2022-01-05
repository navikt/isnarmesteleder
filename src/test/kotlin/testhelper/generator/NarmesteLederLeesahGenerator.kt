package testhelper.generator

import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NY_LEDER
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NarmesteLederLeesah
import testhelper.UserConstants
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generateNarmesteLederLeesah(
    arbeidstakerPersonIdentNumber: PersonIdentNumber = ARBEIDSTAKER_FNR,
    narmestelederPersonIdentNumber: PersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
    status: String? = NY_LEDER,
    timestamp: OffsetDateTime = OffsetDateTime.now().minusDays(5),
    virksomhetsnummer: Virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER_DEFAULT,
) = NarmesteLederLeesah(
    narmesteLederId = UUID.randomUUID(),
    fnr = arbeidstakerPersonIdentNumber.value,
    orgnummer = virksomhetsnummer.value,
    narmesteLederFnr = narmestelederPersonIdentNumber.value,
    narmesteLederTelefonnummer = "99119911",
    narmesteLederEpost = "test@test.com",
    aktivFom = LocalDate.now().minusDays(10),
    aktivTom = null,
    arbeidsgiverForskutterer = null,
    timestamp = timestamp,
    status = status,
)
