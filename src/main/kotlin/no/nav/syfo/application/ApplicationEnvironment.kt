package no.nav.syfo.application

import io.ktor.server.application.*
import no.nav.syfo.application.cache.RedisConfig
import no.nav.syfo.application.database.DatabaseEnvironment
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.ClientsEnvironment
import no.nav.syfo.client.azuread.AzureEnvironment
import java.net.URI

const val NAIS_DATABASE_ENV_PREFIX = "NAIS_DATABASE_ISNARMESTELEDER_ISNARMESTELEDER_DB"

data class Environment(
    val azure: AzureEnvironment = AzureEnvironment(
        appClientId = getEnvVar("AZURE_APP_CLIENT_ID"),
        appClientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
        appPreAuthorizedApps = getEnvVar("AZURE_APP_PRE_AUTHORIZED_APPS"),
        appWellKnownUrl = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
        openidConfigTokenEndpoint = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    ),

    val tokenx: TokenxEnvironment = TokenxEnvironment(
        tokenxClientId = getEnvVar("TOKEN_X_CLIENT_ID"),
        tokenxWellKnownUrl = getEnvVar("TOKEN_X_WELL_KNOWN_URL"),
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

    val redisConfig: RedisConfig = RedisConfig(
        redisUri = URI(getEnvVar("REDIS_URI_CACHE")),
        redisDB = 13, // se https://github.com/navikt/istilgangskontroll/blob/master/README.md
        redisUsername = getEnvVar("REDIS_USERNAME_CACHE"),
        redisPassword = getEnvVar("REDIS_PASSWORD_CACHE"),
    ),

    val clients: ClientsEnvironment = ClientsEnvironment(
        ereg = ClientEnvironment(
            baseUrl = getEnvVar("EREG_URL"),
            clientId = "",
        ),
        pdl = ClientEnvironment(
            baseUrl = getEnvVar("PDL_URL"),
            clientId = getEnvVar("PDL_CLIENT_ID"),
        ),
        tilgangskontroll = ClientEnvironment(
            baseUrl = getEnvVar("ISTILGANGSKONTROLL_URL"),
            clientId = getEnvVar("ISTILGANGSKONTROLL_CLIENT_ID"),
        ),
    ),

    private val syfomotebehovApplicationName: String = "syfomotebehov",
    private val isdialogmoteApplicationName: String = "isdialogmote",
    val systemAPIAuthorizedConsumerApplicationNameList: List<String> = listOf(
        isdialogmoteApplicationName,
        syfomotebehovApplicationName
    ),
)

data class TokenxEnvironment(
    val tokenxClientId: String,
    val tokenxWellKnownUrl: String,
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
