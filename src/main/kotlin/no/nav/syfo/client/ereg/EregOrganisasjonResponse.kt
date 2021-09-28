package no.nav.syfo.client.ereg

data class EregOrganisasjonNavn(
    val navnelinje1: String,
    val redigertnavn: String?,
)

data class EregOrganisasjonResponse(
    val navn: EregOrganisasjonNavn,
)

fun EregOrganisasjonResponse.virksomhetsnavn(): String =
    this.navn.let { (navnelinje1, redigertnavn) ->
        if (redigertnavn.isNullOrBlank()) {
            navnelinje1
        } else {
            redigertnavn
        }
    }
