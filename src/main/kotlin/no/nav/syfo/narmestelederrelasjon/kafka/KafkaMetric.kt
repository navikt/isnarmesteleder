package no.nav.syfo.narmestelederrelasjon.kafka

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val KAFKA_CONSUMER_NARMESTELEDERRELASJON_BASE = "${METRICS_NS}_kafka_consumer_nlr"
const val KAFKA_CONSUMER_NARMESTELEDERRELASJON_READ = "${KAFKA_CONSUMER_NARMESTELEDERRELASJON_BASE}_read"
const val KAFKA_CONSUMER_NARMESTELEDERRELASJON_DUPLICATE = "${KAFKA_CONSUMER_NARMESTELEDERRELASJON_BASE}_duplicate"
const val KAFKA_CONSUMER_NARMESTELEDERRELASJON_TOMBSTONE = "${KAFKA_CONSUMER_NARMESTELEDERRELASJON_BASE}_tombstone"

val COUNT_KAFKA_CONSUMER_NARMESTELEDERRELASJON_READ: Counter = Counter.builder(KAFKA_CONSUMER_NARMESTELEDERRELASJON_READ)
    .description("Counts the number of reads from topic - NarmesteLederRelasjon")
    .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_NARMESTELEDERRELASJON_DUPLICATE: Counter = Counter.builder(KAFKA_CONSUMER_NARMESTELEDERRELASJON_DUPLICATE)
    .description("Counts the number of duplicates received from topic - NarmesteLederRelasjon")
    .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_NARMESTELEDERRELASJON_TOMBSTONE: Counter = Counter.builder(KAFKA_CONSUMER_NARMESTELEDERRELASJON_TOMBSTONE)
    .description("Counts the number of tombstones received from topic - NarmesteLederRelasjon")
    .register(METRICS_REGISTRY)
