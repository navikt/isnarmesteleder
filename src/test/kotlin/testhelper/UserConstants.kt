package testhelper

import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer

object UserConstants {
    val ARBEIDSTAKER_FNR = PersonIdentNumber("12345678912")
    val ARBEIDSTAKER_VEILEDER_NO_ACCESS = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "1"))
    val ARBEIDSTAKER_NO_VIRKSOMHETNAVN = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "3"))

    val NARMESTELEDER_PERSONIDENTNUMBER = PersonIdentNumber("98765432101")
    val NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE = PersonIdentNumber("98765432101")

    val VIRKSOMHETSNUMMER_DEFAULT = Virksomhetsnummer("912345678")
    val VIRKSOMHETSNUMMER_ALTERNATIVE = Virksomhetsnummer(VIRKSOMHETSNUMMER_DEFAULT.value.replace("1", "2"))
    val VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN = Virksomhetsnummer(VIRKSOMHETSNUMMER_DEFAULT.value.replace("1", "3"))

    const val VEILEDER_IDENT = "Z999999"

    const val NARMESTELEDER_FORNAVN = "Leder"
    const val NARMESTELEDER_MELLOMNAVN = "Mellomleder"
    const val NARMESTELEDER_ETTERNAVN = "Ledersen"
}
