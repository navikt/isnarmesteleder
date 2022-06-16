package no.nav.syfo.application.api.authentication

import com.auth0.jwt.JWT
import no.nav.syfo.domain.PersonIdentNumber

const val JWT_CLAIM_NAVIDENT = "NAVident"
const val JWT_CLAIM_AZP = "azp"

fun getConsumerClientId(token: String): String =
    JWT.decode(token).claims[JWT_CLAIM_AZP]?.asString()
        ?: throw IllegalArgumentException("Claim AZP was not found in token")

fun getPersonIdentFromToken(token: String): PersonIdentNumber? {
    val pid = JWT.decode(token).claims["pid"]
    return pid?.asString()?.let { PersonIdentNumber(it) }
}
