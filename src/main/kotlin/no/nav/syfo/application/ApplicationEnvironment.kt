package no.nav.syfo.application

import io.ktor.server.application.*
import no.nav.syfo.application.cache.RedisEnvironment
import no.nav.syfo.application.database.DatabaseEnvironment
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.ClientsEnvironment
import no.nav.syfo.client.azuread.AzureEnvironment

const val NAIS_DATABASE_ENV_PREFIX = "NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER"

data class Environment(
    val azure: AzureEnvironment = AzureEnvironment(
        appClientId = getEnvVar("AZURE_APP_CLIENT_ID"),
        appClientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
        appPreAuthorizedApps = getEnvVar("AZURE_APP_PRE_AUTHORIZED_APPS"),
        appWellKnownUrl = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
        openidConfigTokenEndpoint = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    ),

    val database: DatabaseEnvironment = DatabaseEnvironment(
        host = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_HOST"),
        name = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_DATABASE"),
        port = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PORT"),
        password = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PASSWORD"),
        username = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_USERNAME"),
    ),

    val electorPath: String = getEnvVar("ELECTOR_PATH"),

    val kafka: KafkaEnvironment = KafkaEnvironment(
        aivenBootstrapServers = getEnvVar("KAFKA_BROKERS"),
        aivenCredstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        aivenKeystoreLocation = getEnvVar("KAFKA_KEYSTORE_PATH"),
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    ),

    val redis: RedisEnvironment = RedisEnvironment(
        host = getEnvVar("REDIS_HOST"),
        port = getEnvVar("REDIS_PORT", "6379").toInt(),
        secret = getEnvVar("REDIS_PASSWORD"),
    ),

    val clients: ClientsEnvironment = ClientsEnvironment(
        isproxy = ClientEnvironment(
            baseUrl = getEnvVar("ISPROXY_URL"),
            clientId = getEnvVar("ISPROXY_CLIENT_ID"),
        ),
        pdl = ClientEnvironment(
            baseUrl = getEnvVar("PDL_URL"),
            clientId = getEnvVar("PDL_CLIENT_ID"),
        ),
        syfotilgangskontroll = ClientEnvironment(
            baseUrl = getEnvVar("SYFOTILGANGSKONTROLL_URL"),
            clientId = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
        ),
    ),

    private val syfomoteadminApplicationName: String = "syfomoteadmin",
    private val isdialogmoteApplicationName: String = "isdialogmote",
    val systemAPIAuthorizedConsumerApplicationNameList: List<String> = listOf(
        syfomoteadminApplicationName,
        isdialogmoteApplicationName,
    ),
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
