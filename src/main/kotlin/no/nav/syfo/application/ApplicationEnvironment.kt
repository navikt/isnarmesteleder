package no.nav.syfo.application

import io.ktor.application.*

data class Environment(
    val azureAppClientId: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppClientSecret: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
    val azureOpenidConfigTokenEndpoint: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),

    val electorPath: String = getEnvVar("ELECTOR_PATH"),

    val isnarmestelederDbHost: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_HOST"),
    val isnarmestelederDbPort: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_PORT"),
    val isnarmestelederDbName: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_DATABASE"),
    val isnarmestelederDbUsername: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_USERNAME"),
    val isnarmestelederDbPassword: String = getEnvVar("NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB_PASSWORD"),

    val kafka: ApplicationEnvironmentKafka = ApplicationEnvironmentKafka(
        aivenBootstrapServers = getEnvVar("KAFKA_BROKERS"),
        aivenCredstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        aivenKeystoreLocation = getEnvVar("KAFKA_KEYSTORE_PATH"),
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    ),

    val redisHost: String = getEnvVar("REDIS_HOST"),
    val redisPort: Int = getEnvVar("REDIS_PORT", "6379").toInt(),
    val redisSecret: String = getEnvVar("REDIS_PASSWORD"),

    val isproxyClientId: String = getEnvVar("ISPROXY_CLIENT_ID"),
    val isproxyUrl: String = getEnvVar("ISPROXY_URL"),

    val pdlClientId: String = getEnvVar("PDL_CLIENT_ID"),
    val pdlUrl: String = getEnvVar("PDL_URL"),

    val syfotilgangskontrollClientId: String = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
    val syfotilgangskontrollUrl: String = getEnvVar("SYFOTILGANGSKONTROLL_URL"),
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$isnarmestelederDbHost:$isnarmestelederDbPort/$isnarmestelederDbName"
    }
}

data class ApplicationEnvironmentKafka(
    val aivenBootstrapServers: String,
    val aivenCredstorePassword: String,
    val aivenKeystoreLocation: String,
    val aivenSecurityProtocol: String,
    val aivenTruststoreLocation: String,
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
