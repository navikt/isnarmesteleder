package testhelper.generator

import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NY_LEDER
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NarmesteLederLeesah
import testhelper.UserConstants
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_AKTIV_FOM
import testhelper.UserConstants.NARMESTELEDER_EPOST
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.UserConstants.NARMESTELEDER_TELEFON
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generateNarmesteLederLeesah(
    narmesteLederId: UUID = UUID.randomUUID(),
    arbeidstakerPersonIdentNumber: PersonIdentNumber = ARBEIDSTAKER_FNR,
    narmestelederPersonIdentNumber: PersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
    status: String? = NY_LEDER,
    timestamp: OffsetDateTime = OffsetDateTime.now().minusDays(5),
    virksomhetsnummer: Virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER_DEFAULT,
    aktivFom: LocalDate = NARMESTELEDER_AKTIV_FOM,
    aktivTom: LocalDate? = null,
) = NarmesteLederLeesah(
    narmesteLederId = narmesteLederId,
    fnr = arbeidstakerPersonIdentNumber.value,
    orgnummer = virksomhetsnummer.value,
    narmesteLederFnr = narmestelederPersonIdentNumber.value,
    narmesteLederTelefonnummer = NARMESTELEDER_TELEFON,
    narmesteLederEpost = NARMESTELEDER_EPOST,
    aktivFom = aktivFom,
    aktivTom = aktivTom,
    arbeidsgiverForskutterer = null,
    timestamp = timestamp,
    status = status,
)
