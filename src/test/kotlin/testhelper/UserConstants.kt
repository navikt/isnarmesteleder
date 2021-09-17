package testhelper

import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer

object UserConstants {
    val ARBEIDSTAKER_FNR = PersonIdentNumber("12345678912")
    val ARBEIDSTAKER_VEILEDER_NO_ACCESS = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "1"))

    val NARMESTELEDER_PERSONIDENTNUMBER = PersonIdentNumber("98765432101")
    val NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE = PersonIdentNumber("98765432101")

    val VIRKSOMHETSNUMMER_DEFAULT = Virksomhetsnummer("912345678")
    val VIRKSOMHETSNUMMER_ALTERNATIVE = Virksomhetsnummer(VIRKSOMHETSNUMMER_DEFAULT.value.replace("1", "2"))

    const val VEILEDER_IDENT = "Z999999"
}
