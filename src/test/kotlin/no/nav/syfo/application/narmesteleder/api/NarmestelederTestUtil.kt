package no.nav.syfo.application.narmesteleder.api

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.narmestelederrelasjon.kafka.NARMESTE_LEDER_RELASJON_TOPIC
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NY_LEDER
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import testhelper.UserConstants
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.generator.generateNarmesteLederLeesah
import testhelper.mock.toHistoricalPersonIdentNumber
import java.time.OffsetDateTime

fun generateNarmestelederTestdata(): ConsumerRecords<String, String> {
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    val partition = 0
    val narmesteLederRelasjonTopicPartition = TopicPartition(
        NARMESTE_LEDER_RELASJON_TOPIC,
        partition,
    )
    val narmesteLederLeesah = generateNarmesteLederLeesah(
        arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR.toHistoricalPersonIdentNumber(),
        status = null,
        timestamp = OffsetDateTime.now().minusDays(1),
    )
    val narmesteLederLeesahRecord = ConsumerRecord(
        NARMESTE_LEDER_RELASJON_TOPIC,
        partition,
        1,
        "something",
        objectMapper.writeValueAsString(narmesteLederLeesah),
    )
    val narmesteLederLeesahRecordDuplicate = ConsumerRecord(
        NARMESTE_LEDER_RELASJON_TOPIC,
        partition,
        1,
        "something",
        objectMapper.writeValueAsString(narmesteLederLeesah),
    )
    val ansattLeesah = generateNarmesteLederLeesah(
        arbeidstakerPersonIdentNumber = UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
        narmestelederPersonIdentNumber = ARBEIDSTAKER_FNR.toHistoricalPersonIdentNumber(),
        status = null,
        timestamp = OffsetDateTime.now().minusDays(1),
    )
    val ansattLeesahRecord = ConsumerRecord(
        NARMESTE_LEDER_RELASJON_TOPIC,
        partition,
        3,
        "something",
        objectMapper.writeValueAsString(ansattLeesah),
    )
    val narmesteLederLeesahNoVirksomhetsnavn = generateNarmesteLederLeesah(
        arbeidstakerPersonIdentNumber = UserConstants.ARBEIDSTAKER_NO_VIRKSOMHETNAVN,
        virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN,
        status = NY_LEDER,
    )
    val narmesteLederLeesahRecordNoVirksomhetsnavn = ConsumerRecord(
        NARMESTE_LEDER_RELASJON_TOPIC,
        partition,
        2,
        "something",
        objectMapper.writeValueAsString(narmesteLederLeesahNoVirksomhetsnavn),
    )
    return ConsumerRecords(
        mapOf(
            narmesteLederRelasjonTopicPartition to listOf(
                narmesteLederLeesahRecord,
                narmesteLederLeesahRecordDuplicate,
                narmesteLederLeesahRecordNoVirksomhetsnavn,
                ansattLeesahRecord,
            )
        )
    )
}
