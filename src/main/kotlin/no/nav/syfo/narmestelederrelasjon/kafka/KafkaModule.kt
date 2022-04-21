package no.nav.syfo.narmestelederrelasjon.kafka

import kotlinx.coroutines.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.kafka.KafkaEnvironment
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.narmestelederrelasjon.kafka")

fun launchBackgroundTask(
    applicationState: ApplicationState,
    action: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch {
    try {
        action()
    } catch (ex: Exception) {
        log.error("Exception received while launching background task. Terminating application.", ex)
    } finally {
        applicationState.alive = false
        applicationState.ready = false
    }
}

fun launchKafkaTask(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    database: DatabaseInterface,
) {
    launchBackgroundTask(applicationState = applicationState) {
        blockingApplicationLogicNarmesteLederRelasjon(
            applicationState = applicationState,
            kafkaEnvironment = kafkaEnvironment,
            database = database,
        )
    }
}
