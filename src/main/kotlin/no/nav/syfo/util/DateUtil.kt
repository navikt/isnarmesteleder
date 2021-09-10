package no.nav.syfo.util

import java.sql.Timestamp
import java.time.*

val defaultZoneOffset: ZoneOffset = ZoneOffset.UTC

fun nowTimestampUTC(): Timestamp = Timestamp.from(OffsetDateTime.now(defaultZoneOffset).toInstant())

fun Timestamp.toOffsetDateTimeUTC(): OffsetDateTime = this.toInstant().atOffset(defaultZoneOffset)

fun OffsetDateTime.toLocalDateTimeOslo(): LocalDateTime = this.atZoneSameInstant(
    ZoneId.of("Europe/Oslo")
).toLocalDateTime()
