package no.nav.syfo.application.narmesteleder.api

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.narmestelederrelasjon.kafka.NARMESTE_LEDER_RELASJON_TOPIC
import no.nav.syfo.narmestelederrelasjon.kafka.domain.DEAKTIVERT_LEDER
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NY_LEDER
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import testhelper.UserConstants
import testhelper.UserConstants.ARBEIDSTAKER_FNR
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE
import testhelper.generator.generateNarmesteLederLeesah
import testhelper.mock.toHistoricalPersonIdentNumber
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

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

fun generateNarmestelederTestdataMedLederBytte(): ConsumerRecords<String, String> {
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    val partition = 0
    val narmesteLederRelasjonTopicPartition = TopicPartition(
        NARMESTE_LEDER_RELASJON_TOPIC,
        partition,
    )
    val narmesteLederId = UUID.randomUUID()
    val narmesteLeder = generateNarmesteLederLeesah(
        narmesteLederId = narmesteLederId,
        arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR,
        status = null,
        timestamp = OffsetDateTime.now().minusDays(365),
        aktivFom = LocalDate.now().minusDays(365)
    )
    val narmesteLederSlutt = generateNarmesteLederLeesah(
        narmesteLederId = narmesteLederId,
        arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR,
        status = DEAKTIVERT_LEDER,
        timestamp = OffsetDateTime.now().minusDays(100),
        aktivFom = LocalDate.now().minusDays(365),
        aktivTom = LocalDate.now().minusDays(100),
    )
    val narmesteLederNyId = UUID.randomUUID()
    val narmesteLederNy = generateNarmesteLederLeesah(
        narmesteLederId = narmesteLederNyId,
        arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR,
        narmestelederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
        status = NY_LEDER,
        timestamp = OffsetDateTime.now().minusDays(99),
        aktivFom = LocalDate.now().minusDays(99),
    )
    val narmesteLedeNyDuplikat = generateNarmesteLederLeesah(
        narmesteLederId = narmesteLederNyId,
        arbeidstakerPersonIdentNumber = ARBEIDSTAKER_FNR,
        narmestelederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
        status = NY_LEDER,
        timestamp = OffsetDateTime.now().minusDays(1),
        aktivFom = LocalDate.now().minusDays(99),
    )
    return ConsumerRecords(
        mapOf(
            narmesteLederRelasjonTopicPartition to listOf(
                Pair(1L, narmesteLeder),
                Pair(2L, narmesteLederSlutt),
                Pair(3L, narmesteLederNy),
                Pair(4L, narmesteLedeNyDuplikat),
            ).map {
                ConsumerRecord(
                    NARMESTE_LEDER_RELASJON_TOPIC,
                    partition,
                    it.first,
                    "something",
                    objectMapper.writeValueAsString(it.second),
                )
            }
        )
    )
}
