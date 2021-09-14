package no.nav.syfo.application

import io.ktor.application.*

data class Environment(
    val azureAppClientId: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppClientSecret: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
    val azureOpenidConfigTokenEndpoint: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),

    val isnarmestelederDbHost: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_HOST"),
    val isnarmestelederDbPort: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_PORT"),
    val isnarmestelederDbName: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_DATABASE"),
    val isnarmestelederDbUsername: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_USERNAME"),
    val isnarmestelederDbPassword: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_PASSWORD"),

    val syfotilgangskontrollClientId: String = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
    val syfotilgangskontrollUrl: String = getEnvVar("SYFOTILGANGSKONTROLL_URL"),
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$isnarmestelederDbHost:$isnarmestelederDbPort/$isnarmestelederDbName"
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
