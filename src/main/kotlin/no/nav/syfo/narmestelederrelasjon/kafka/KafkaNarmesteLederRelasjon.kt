package no.nav.syfo.narmestelederrelasjon.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.narmestelederrelasjon.database.NoElementInsertedException
import no.nav.syfo.narmestelederrelasjon.database.createNarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NarmesteLederLeesah
import no.nav.syfo.util.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.narmestelederrelasjon.kafka")

const val NARMESTE_LEDER_RELASJON_TOPIC = "teamsykmelding.syfo-narmesteleder-leesah"

fun blockingApplicationLogicNarmesteLederRelasjon(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
    database: DatabaseInterface,
) {
    log.info("Setting up kafka consumer NarmesteLederLeesah")

    val consumerProperties = kafkaNarmesteLederRelasjonConsumerConfig(applicationEnvironmentKafka)
    val kafkaConsumerNarmesteLederRelasjon = KafkaConsumer<String, String>(consumerProperties)

    kafkaConsumerNarmesteLederRelasjon.subscribe(
        listOf(NARMESTE_LEDER_RELASJON_TOPIC)
    )
    while (applicationState.ready) {
        pollAndProcessNarmesteLederRelasjon(
            database = database,
            kafkaConsumerNarmesteLederRelasjon = kafkaConsumerNarmesteLederRelasjon,
        )
    }
}

fun pollAndProcessNarmesteLederRelasjon(
    database: DatabaseInterface,
    kafkaConsumerNarmesteLederRelasjon: KafkaConsumer<String, String>,
) {
    val records = kafkaConsumerNarmesteLederRelasjon.poll(Duration.ofMillis(1000))
    if (records.count() > 0) {
        createAndStoreNarmesteLederRelasjonFromRecords(
            consumerRecords = records,
            database = database,
        )
        kafkaConsumerNarmesteLederRelasjon.commitSync()
    }
}

fun createAndStoreNarmesteLederRelasjonFromRecords(
    consumerRecords: ConsumerRecords<String, String>,
    database: DatabaseInterface,
) {
    database.connection.use { connection ->
        consumerRecords.forEach { consumerRecord ->
            val callId = kafkaCallId()
            if (consumerRecord.value() == null) {
                log.error("Value of ConsumerRecord is null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected. key=${consumerRecord.key()} from topic: ${consumerRecord.topic()}, partiion=${consumerRecord.partition()}, offset=${consumerRecord.offset()}")
                COUNT_KAFKA_CONSUMER_NARMESTELEDERRELASJON_TOMBSTONE.increment()
                return
            }
            val narmesteLederLeesah: NarmesteLederLeesah = configuredJacksonMapper()
                .readValue(consumerRecord.value())

            COUNT_KAFKA_CONSUMER_NARMESTELEDERRELASJON_READ.increment()
            log.info("Received NarmesteLederLeesah, ready to process. id=${consumerRecord.key()}, timestamp=${consumerRecord.timestamp()}, callId=${callIdArgument(callId)}")

            try {
                connection.createNarmesteLederRelasjon(
                    commit = false,
                    narmesteLederLeesah = narmesteLederLeesah,
                )
            } catch (noElementInsertedException: NoElementInsertedException) {
                log.warn("No NarmesteLederRelasjon was inserted into database, probably due to an attempt to insert a duplicate", noElementInsertedException)
                COUNT_KAFKA_CONSUMER_NARMESTELEDERRELASJON_DUPLICATE.increment()
            }
        }
        connection.commit()
    }
}
