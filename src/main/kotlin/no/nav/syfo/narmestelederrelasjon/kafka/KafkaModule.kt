package no.nav.syfo.narmestelederrelasjon.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.application.launchBackgroundTask

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
