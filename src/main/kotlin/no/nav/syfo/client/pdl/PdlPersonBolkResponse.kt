package no.nav.syfo.client.pdl

import no.nav.syfo.util.lowerCapitalize

data class PdlPersonBolkResponse(
    val data: PdlHentPersonBolkData,
    val errors: List<PdlError>?,
)

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension,
)

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?,
)

data class PdlErrorExtension(
    val code: String?,
    val classification: String,
)

data class PdlHentPersonBolkData(
    val hentPersonBolk: List<PdlHentPerson>?,
)

data class PdlHentPerson(
    val ident: String,
    val person: PdlPerson?,
    val code: String,
)

data class PdlPerson(
    val navn: List<PdlPersonNavn>,
)

data class PdlPersonNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

fun PdlPerson.fullName(): String? {
    val nameList = this.navn
    if (nameList.isNullOrEmpty()) {
        return null
    }
    nameList.first().let {
        val fornavn = it.fornavn.lowerCapitalize()
        val mellomnavn = it.mellomnavn
        val etternavn = it.etternavn.lowerCapitalize()

        return if (mellomnavn.isNullOrBlank()) {
            "$fornavn $etternavn"
        } else {
            "$fornavn ${mellomnavn.lowerCapitalize()} $etternavn"
        }
    }
}

fun PdlError.errorMessage(): String {
    return "${this.message} with code: ${extensions.code} and classification: ${extensions.classification}"
}
