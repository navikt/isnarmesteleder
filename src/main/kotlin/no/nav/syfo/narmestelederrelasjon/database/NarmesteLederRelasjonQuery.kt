package no.nav.syfo.narmestelederrelasjon.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.narmestelederrelasjon.database.domain.PNarmesteLederRelasjon
import no.nav.syfo.narmestelederrelasjon.kafka.domain.NarmesteLederLeesah
import no.nav.syfo.util.*
import java.sql.*
import java.util.*

const val queryCreateNarmesteLederRelasjon =
    """
    INSERT INTO NARMESTE_LEDER_RELASJON (
        id,
        uuid,
        created_at,
        updated_at,
        referanse_uuid,
        virksomhetsnummer,
        arbeidstaker_personident,
        narmeste_leder_personident,
        narmeste_leder_telefonnummer,
        narmeste_leder_epost,
        arbeidsgiver_forskutterer,
        aktiv_fom,
        aktiv_tom,
        timestamp
        ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT DO NOTHING
        RETURNING id
    """

fun Connection.createNarmesteLederRelasjon(
    commit: Boolean,
    narmesteLederLeesah: NarmesteLederLeesah,
) {
    val narmesteLederRelasjonUuid = UUID.randomUUID()

    val narmesteLederRelasjonIdList = this.prepareStatement(queryCreateNarmesteLederRelasjon).use {
        it.setString(1, narmesteLederRelasjonUuid.toString())
        it.setTimestamp(2, nowTimestampUTC())
        it.setTimestamp(3, nowTimestampUTC())
        it.setString(4, narmesteLederLeesah.narmesteLederId.toString())
        it.setString(5, narmesteLederLeesah.orgnummer)
        it.setString(6, narmesteLederLeesah.fnr)
        it.setString(7, narmesteLederLeesah.narmesteLederFnr)
        it.setString(8, narmesteLederLeesah.narmesteLederTelefonnummer)
        it.setString(9, narmesteLederLeesah.narmesteLederEpost)
        it.setObject(10, narmesteLederLeesah.arbeidsgiverForskutterer)
        it.setTimestamp(11, Timestamp.valueOf(narmesteLederLeesah.aktivFom.atStartOfDay()))
        it.setTimestamp(12, narmesteLederLeesah.aktivTom?.let { aktivTom -> Timestamp.valueOf(aktivTom.atStartOfDay()) })
        it.setTimestamp(13, Timestamp.from(narmesteLederLeesah.timestamp.toInstant()))
        it.executeQuery().toList { getInt("id") }
    }

    if (narmesteLederRelasjonIdList.size != 1) {
        throw NoElementInsertedException("Creating NARMESTE_LEDER_RELASJON failed, no rows affected.")
    }

    if (commit) {
        this.commit()
    }
}

const val queryGetNarmesteLederRelasjonList =
    """
    SELECT *
    FROM narmeste_leder_relasjon
    WHERE arbeidstaker_personident = ?;
    """

fun DatabaseInterface.getNarmesteLederRelasjonList(
    personIdentNumber: PersonIdentNumber,
): List<PNarmesteLederRelasjon> {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetNarmesteLederRelasjonList).use {
            it.setString(1, personIdentNumber.value)
            it.executeQuery().toList {
                toPNarmesteLederRelasjon()
            }
        }
    }
}

fun ResultSet.toPNarmesteLederRelasjon(): PNarmesteLederRelasjon =
    PNarmesteLederRelasjon(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        createdAt = getTimestamp("created_at").toOffsetDateTimeUTC(),
        updatedAt = getTimestamp("updated_at").toOffsetDateTimeUTC(),
        referanseUuid = UUID.fromString(getString("referanse_uuid")),
        virksomhetsnavn = getString("virksomhetsnavn"),
        virksomhetsnummer = Virksomhetsnummer(getString("virksomhetsnummer")),
        arbeidstakerPersonIdentNumber = PersonIdentNumber(getString("arbeidstaker_personident")),
        narmesteLederPersonIdentNumber = PersonIdentNumber(getString("narmeste_leder_personident")),
        narmesteLederTelefonnummer = getString("narmeste_leder_telefonnummer"),
        narmesteLederEpost = getString("narmeste_leder_epost"),
        arbeidsgiverForskutterer = getObject("arbeidsgiver_forskutterer") as Boolean?,
        aktivFom = getDate("aktiv_fom").toLocalDate(),
        aktivTom = getDate("aktiv_tom")?.toLocalDate(),
        timestamp = getTimestamp("timestamp").toOffsetDateTimeUTC(),
    )
