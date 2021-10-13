package no.nav.syfo.application.narmestelederrelasjon.domain

import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
import no.nav.syfo.narmestelederrelasjon.kafka.domain.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE
import testhelper.UserConstants.VIRKSOMHETSNUMMER_ALTERNATIVE
import testhelper.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import testhelper.generator.generatePNarmesteLederRelasjon
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class PNarmesteLederRelasjonSpek : Spek({
    describe(PNarmesteLederRelasjonSpek::class.java.simpleName) {

        val basePNarmesteLederRelasjon = generatePNarmesteLederRelasjon()

        describe("Map list of PNarmesteLederRelasjon to list of NarmesteLederRelasjon: Multiple NarmesteLeder and multiple Virksomhetsnummer") {
            it("with 2 Innmeldt and Deaktivert, and then 1 Innmeldt should return list with 1 ${NarmesteLederRelasjonStatus.INNMELDT_AKTIV} for each Virksomhetsnummer") {
                val firstInnmeldtVirksomhetsnummer1ReferanseUUid = UUID.randomUUID()
                val firstInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = 1,
                    referanseUuid = firstInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = null,
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = OffsetDateTime.now().minusDays(5),
                    status = NY_LEDER,

                )
                val firstDeaktivertVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = firstInnmeldtVirksomhetsnummer1.id + 1,
                    referanseUuid = firstInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = LocalDate.now().minusDays(10),
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = firstInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                    status = DEAKTIVERT_NY_LEDER,
                )
                val secondInnmeldtVirksomhetsnummer1ReferanseUUid = UUID.randomUUID()
                val secondInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = firstDeaktivertVirksomhetsnummer1.id + 1,
                    referanseUuid = secondInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = null,
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = firstDeaktivertVirksomhetsnummer1.timestamp.plusDays(1),
                    status = NY_LEDER,
                )
                val secondDeaktivertVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = secondInnmeldtVirksomhetsnummer1.id + 1,
                    referanseUuid = secondInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = LocalDate.now().minusDays(10),
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = secondInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                    status = DEAKTIVERT_ARBEIDSTAKER,
                )
                val lastInnmeldtVirksomhetsnummer1ReferanseUUid = UUID.randomUUID()
                val lastInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = secondDeaktivertVirksomhetsnummer1.id + 1,
                    referanseUuid = lastInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = null,
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = secondDeaktivertVirksomhetsnummer1.timestamp.plusDays(1),
                    status = NY_LEDER,
                )
                val inputListVirksomhetsnummer1 = listOf(
                    firstInnmeldtVirksomhetsnummer1,
                    firstDeaktivertVirksomhetsnummer1,
                    secondInnmeldtVirksomhetsnummer1,
                    secondDeaktivertVirksomhetsnummer1,
                    lastInnmeldtVirksomhetsnummer1,
                )

                val firstInnmeldtVirksomhetsnummer2ReferanseUUid = UUID.randomUUID()
                val firstInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = inputListVirksomhetsnummer1.size + 1,
                    referanseUuid = firstInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = OffsetDateTime.now().minusDays(5),
                    status = NY_LEDER,
                )
                val firstDeaktivertVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = firstInnmeldtVirksomhetsnummer2.id + 1,
                    referanseUuid = firstInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = LocalDate.now().minusDays(10),
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = firstInnmeldtVirksomhetsnummer2.timestamp.plusDays(1),
                    status = DEAKTIVERT_LEDER,
                )
                val secondInnmeldtVirksomhetsnummer2ReferanseUUid = UUID.randomUUID()
                val secondInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = firstDeaktivertVirksomhetsnummer2.id + 1,
                    referanseUuid = secondInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = firstDeaktivertVirksomhetsnummer2.timestamp.plusDays(1),
                    status = NY_LEDER,
                )
                val secondDeaktivertVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = secondInnmeldtVirksomhetsnummer2.id + 1,
                    referanseUuid = secondInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = LocalDate.now().minusDays(10),
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = secondInnmeldtVirksomhetsnummer2.timestamp.plusDays(1),
                    status = DEAKTIVERT_ARBEIDSFORHOLD,
                )
                val lastInnmeldtVirksomhetsnummer2ReferanseUUid = UUID.randomUUID()
                val lastInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = secondDeaktivertVirksomhetsnummer2.id + 1,
                    referanseUuid = lastInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = secondDeaktivertVirksomhetsnummer2.timestamp.plusDays(1),
                    status = NY_LEDER,
                )
                val inputListVirksomhetsnummer2 = listOf(
                    firstInnmeldtVirksomhetsnummer2,
                    firstDeaktivertVirksomhetsnummer2,
                    secondInnmeldtVirksomhetsnummer2,
                    secondDeaktivertVirksomhetsnummer2,
                    lastInnmeldtVirksomhetsnummer2,
                )
                val outputList = inputListVirksomhetsnummer2.plus(inputListVirksomhetsnummer1).toNarmesteLederRelasjonList()

                val outputListVirksomhetsnummer1 = outputList.filter {
                    it.virksomhetsnummer.value == VIRKSOMHETSNUMMER_DEFAULT.value
                }
                outputListVirksomhetsnummer1.size shouldBeEqualTo inputListVirksomhetsnummer1.distinctBy { it.referanseUuid }.size

                outputListVirksomhetsnummer1[0].id shouldBeEqualTo lastInnmeldtVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[0].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_AKTIV

                outputListVirksomhetsnummer1[1].id shouldBeEqualTo secondDeaktivertVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[1].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT_ARBEIDSTAKER

                outputListVirksomhetsnummer1[2].id shouldBeEqualTo firstDeaktivertVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[2].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT_NY_LEDER

                val outputListVirksomhetsnummer2 = outputList.filter {
                    it.virksomhetsnummer.value == VIRKSOMHETSNUMMER_ALTERNATIVE.value
                }
                outputListVirksomhetsnummer2.size shouldBeEqualTo inputListVirksomhetsnummer2.distinctBy { it.referanseUuid }.size

                outputListVirksomhetsnummer2[0].id shouldBeEqualTo lastInnmeldtVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[0].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_AKTIV

                outputListVirksomhetsnummer2[1].id shouldBeEqualTo secondDeaktivertVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[1].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT_ARBEIDSFORHOLD

                outputListVirksomhetsnummer2[2].id shouldBeEqualTo firstDeaktivertVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[2].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT_LEDER
            }

            it("with 2 Innmeldt and 1 Deaktivert should return list without ${NarmesteLederRelasjonStatus.INNMELDT_AKTIV}") {
                val firstInnmeldtVirksomhetsnummer1ReferanseUUid = UUID.randomUUID()
                val firstInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = 1,
                    referanseUuid = firstInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = null,
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = OffsetDateTime.now().minusDays(5),
                    status = NY_LEDER,
                )
                val firstDeaktivertVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = firstInnmeldtVirksomhetsnummer1.id + 1,
                    referanseUuid = firstInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = LocalDate.now().minusDays(10),
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = firstInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                    status = null,
                )
                val secondInnmeldtVirksomhetsnummer1ReferanseUUid = UUID.randomUUID()
                val secondInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = firstDeaktivertVirksomhetsnummer1.id + 1,
                    referanseUuid = secondInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = null,
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = firstDeaktivertVirksomhetsnummer1.timestamp.plusDays(1),
                    status = NY_LEDER,
                )
                val secondDeaktivertVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = secondInnmeldtVirksomhetsnummer1.id + 1,
                    referanseUuid = secondInnmeldtVirksomhetsnummer1ReferanseUUid,
                    aktivTom = LocalDate.now().minusDays(10),
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = secondInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                    status = DEAKTIVERT_ARBEIDSTAKER,
                )
                val inputListVirksomhetsnummer1 = listOf(
                    firstInnmeldtVirksomhetsnummer1,
                    firstDeaktivertVirksomhetsnummer1,
                    secondInnmeldtVirksomhetsnummer1,
                    secondDeaktivertVirksomhetsnummer1,
                )

                val firstInnmeldtVirksomhetsnummer2ReferanseUUid = UUID.randomUUID()
                val firstInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = inputListVirksomhetsnummer1.size + 1,
                    referanseUuid = firstInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = OffsetDateTime.now().minusDays(5),
                    status = NY_LEDER,
                )
                val firstDeaktivertVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = firstInnmeldtVirksomhetsnummer2.id + 1,
                    referanseUuid = firstInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = LocalDate.now().minusDays(10),
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = firstInnmeldtVirksomhetsnummer2.timestamp.plusDays(1),
                    status = null,
                )
                val secondInnmeldtVirksomhetsnummer2ReferanseUUid = UUID.randomUUID()
                val secondInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = firstDeaktivertVirksomhetsnummer2.id + 1,
                    referanseUuid = secondInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = firstDeaktivertVirksomhetsnummer2.timestamp.plusDays(1),
                    status = NY_LEDER,
                )
                val secondDeaktivertVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = secondInnmeldtVirksomhetsnummer2.id + 1,
                    referanseUuid = secondInnmeldtVirksomhetsnummer2ReferanseUUid,
                    aktivTom = LocalDate.now().minusDays(10),
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = secondInnmeldtVirksomhetsnummer2.timestamp.plusDays(1),
                    status = DEAKTIVERT_ARBEIDSTAKER_INNSENDT_SYKMELDING,
                )
                val inputListVirksomhetsnummer2 = listOf(
                    firstInnmeldtVirksomhetsnummer2,
                    firstDeaktivertVirksomhetsnummer2,
                    secondInnmeldtVirksomhetsnummer2,
                    secondDeaktivertVirksomhetsnummer2,
                )
                val outputList = inputListVirksomhetsnummer2.plus(inputListVirksomhetsnummer1).toNarmesteLederRelasjonList()

                val outputListVirksomhetsnummer1 = outputList.filter {
                    it.virksomhetsnummer.value == VIRKSOMHETSNUMMER_DEFAULT.value
                }
                outputListVirksomhetsnummer1.size shouldBeEqualTo inputListVirksomhetsnummer1.distinctBy { it.referanseUuid }.size

                outputListVirksomhetsnummer1[0].id shouldBeEqualTo secondDeaktivertVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[0].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT_ARBEIDSTAKER

                outputListVirksomhetsnummer1[1].id shouldBeEqualTo firstDeaktivertVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[1].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT

                val outputListVirksomhetsnummer2 = outputList.filter {
                    it.virksomhetsnummer.value == VIRKSOMHETSNUMMER_ALTERNATIVE.value
                }
                outputListVirksomhetsnummer2.size shouldBeEqualTo inputListVirksomhetsnummer2.distinctBy { it.referanseUuid }.size

                outputListVirksomhetsnummer2[0].id shouldBeEqualTo secondDeaktivertVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[0].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT_ARBEIDSTAKER_INNSENDT_SYKMELDING

                outputListVirksomhetsnummer2[1].id shouldBeEqualTo firstDeaktivertVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[1].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT
            }
        }
    }
})
