package testhelper

import no.nav.common.KafkaEnvironment
import no.nav.syfo.narmestelederrelasjon.kafka.NARMESTE_LEDER_RELASJON_TOPIC

fun testKafka(
    autoStart: Boolean = false,
    withSchemaRegistry: Boolean = false,
    topicNames: List<String> = listOf(
        NARMESTE_LEDER_RELASJON_TOPIC,
    ),
) = KafkaEnvironment(
    autoStart = autoStart,
    withSchemaRegistry = withSchemaRegistry,
    topicNames = topicNames,
)
