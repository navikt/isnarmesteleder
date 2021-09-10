package testhelper

import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer

object UserConstants {
    val ARBEIDSTAKER_FNR = PersonIdentNumber("12345678912")

    val NARMESTELEDER_PERSONIDENTNUMBER = PersonIdentNumber("98765432101")

    val VIRKSOMHETSNUMMER_DEFAULT = Virksomhetsnummer("912345678")

    const val VEILEDER_IDENT = "Z999999"
}
