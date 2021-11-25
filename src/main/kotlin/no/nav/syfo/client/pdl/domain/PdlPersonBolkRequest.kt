package no.nav.syfo.client.pdl

data class PdlPersonBolkRequest(
    val query: String,
    val variables: PdlPersonBolkVariables,
)

data class PdlPersonBolkVariables(
    val identer: List<String>,
)
