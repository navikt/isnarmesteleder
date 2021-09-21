package no.nav.syfo.client.ereg

data class EregOrganisasjonNavn(
    val navnelinje1: String,
    val redigertnavn: String?,
)

data class EregOrganisasjonResponse(
    val navn: EregOrganisasjonNavn,
)
