package no.nav.syfo.client

data class ClientsEnvironment(
    val ereg: ClientEnvironment,
    val pdl: ClientEnvironment,
    val tilgangskontroll: ClientEnvironment,
)

data class ClientEnvironment(
    val baseUrl: String,
    val clientId: String,
)
