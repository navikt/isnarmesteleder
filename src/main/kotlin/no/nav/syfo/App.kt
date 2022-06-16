package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.database.applicationDatabase
import no.nav.syfo.application.database.databaseModule
import no.nav.syfo.client.wellknown.getWellKnown
import no.nav.syfo.cronjob.cronjobModule
import no.nav.syfo.narmestelederrelasjon.kafka.launchKafkaTask
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

const val applicationPort = 8080

fun main() {
    val applicationState = ApplicationState()
    val environment = Environment()

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = applicationPort
        }

        val wellKnownInternalAzureAD = getWellKnown(
            wellKnownUrl = environment.azure.appWellKnownUrl
        )

        val wellKnownSelvbetjening = getWellKnown(
            wellKnownUrl = environment.tokenx.tokenxWellKnownUrl
        )

        module {
            databaseModule(
                databaseEnvironment = environment.database,
            )
            apiModule(
                applicationState = applicationState,
                database = applicationDatabase,
                environment = environment,
                wellKnownInternalAzureAD = wellKnownInternalAzureAD,
                wellKnownSelvbetjening = wellKnownSelvbetjening,
            )
        }
    }

    val server = embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment,
    )

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) { application ->
        applicationState.ready = true
        application.environment.log.info("Application is ready, running Java VM ${Runtime.version()}")
        launchKafkaTask(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
            database = applicationDatabase,
        )
        cronjobModule(
            applicationState = applicationState,
            database = applicationDatabase,
            environment = environment,
        )
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = true)
}
