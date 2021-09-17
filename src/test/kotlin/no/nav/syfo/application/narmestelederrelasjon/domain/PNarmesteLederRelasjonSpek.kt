package no.nav.syfo.application.narmestelederrelasjon.domain

import no.nav.syfo.narmestelederrelasjon.database.domain.toNarmesteLederRelasjonList
import no.nav.syfo.narmestelederrelasjon.domain.NarmesteLederRelasjonStatus
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
            it("with 2 Innmeldt, 1 Deaktivert and 1 Innmeldt should return list with 1 ${NarmesteLederRelasjonStatus.INNMELDT_AKTIV}") {
                val firstInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = OffsetDateTime.now().minusDays(5),
                )
                val secondInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = firstInnmeldtVirksomhetsnummer1.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = firstInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                )
                val deaktivertVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = secondInnmeldtVirksomhetsnummer1.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = LocalDate.now().minusDays(10),
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER_ALTERNATIVE,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = secondInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                )
                val lastInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = deaktivertVirksomhetsnummer1.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    narmesteLederPersonIdentNumber = NARMESTELEDER_PERSONIDENTNUMBER,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = deaktivertVirksomhetsnummer1.timestamp.plusDays(1),
                )
                val inputListVirksomhetsnummer1 = listOf(
                    firstInnmeldtVirksomhetsnummer1,
                    secondInnmeldtVirksomhetsnummer1,
                    deaktivertVirksomhetsnummer1,
                    lastInnmeldtVirksomhetsnummer1,
                )
                val firstInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = inputListVirksomhetsnummer1.size + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = OffsetDateTime.now().minusDays(5),
                )
                val secondInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = firstInnmeldtVirksomhetsnummer2.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = firstInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                )
                val deaktivertVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = secondInnmeldtVirksomhetsnummer2.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = LocalDate.now().minusDays(10),
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = secondInnmeldtVirksomhetsnummer2.timestamp.plusDays(1),
                )
                val lastInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = deaktivertVirksomhetsnummer2.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = deaktivertVirksomhetsnummer2.timestamp.plusDays(1),
                )
                val inputListVirksomhetsnummer2 = listOf(
                    firstInnmeldtVirksomhetsnummer2,
                    secondInnmeldtVirksomhetsnummer2,
                    deaktivertVirksomhetsnummer2,
                    lastInnmeldtVirksomhetsnummer2,
                )
                val outputList = inputListVirksomhetsnummer2.plus(inputListVirksomhetsnummer1).toNarmesteLederRelasjonList()

                val outputListVirksomhetsnummer1 = outputList.filter {
                    it.virksomhetsnummer.value == VIRKSOMHETSNUMMER_DEFAULT.value
                }
                outputListVirksomhetsnummer1.size shouldBeEqualTo inputListVirksomhetsnummer1.size

                outputListVirksomhetsnummer1[0].id shouldBeEqualTo lastInnmeldtVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[0].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_AKTIV

                outputListVirksomhetsnummer1[1].id shouldBeEqualTo deaktivertVirksomhetsnummer1.id
                outputList[1].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT

                outputListVirksomhetsnummer1[2].id shouldBeEqualTo secondInnmeldtVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[2].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_INAKTIV

                outputListVirksomhetsnummer1[3].id shouldBeEqualTo firstInnmeldtVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[3].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_INAKTIV

                val outputListVirksomhetsnummer2 = outputList.filter {
                    it.virksomhetsnummer.value == VIRKSOMHETSNUMMER_ALTERNATIVE.value
                }
                outputListVirksomhetsnummer2.size shouldBeEqualTo inputListVirksomhetsnummer2.size

                outputListVirksomhetsnummer2[0].id shouldBeEqualTo lastInnmeldtVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[0].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_AKTIV

                outputListVirksomhetsnummer2[1].id shouldBeEqualTo deaktivertVirksomhetsnummer2.id
                outputList[1].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT

                outputListVirksomhetsnummer2[2].id shouldBeEqualTo secondInnmeldtVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[2].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_INAKTIV

                outputListVirksomhetsnummer2[3].id shouldBeEqualTo firstInnmeldtVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[3].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_INAKTIV
            }

            it("with 2 Innmeldt and 1 Deaktivert should return list without ${NarmesteLederRelasjonStatus.INNMELDT_AKTIV}") {
                val firstInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = OffsetDateTime.now().minusDays(5),
                )
                val secondInnmeldtVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = firstInnmeldtVirksomhetsnummer1.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = firstInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                )
                val deaktivertVirksomhetsnummer1 = basePNarmesteLederRelasjon.copy(
                    id = secondInnmeldtVirksomhetsnummer1.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = LocalDate.now().minusDays(10),
                    virksomhetsnummer = VIRKSOMHETSNUMMER_DEFAULT,
                    timestamp = secondInnmeldtVirksomhetsnummer1.timestamp.plusDays(1),
                )
                val inputListVirksomhetsnummer1 = listOf(
                    firstInnmeldtVirksomhetsnummer1,
                    secondInnmeldtVirksomhetsnummer1,
                    deaktivertVirksomhetsnummer1,
                )
                val firstInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = inputListVirksomhetsnummer1.size + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = OffsetDateTime.now().minusDays(5),
                )
                val secondInnmeldtVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = firstInnmeldtVirksomhetsnummer2.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = null,
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = firstInnmeldtVirksomhetsnummer2.timestamp.plusDays(1),
                )
                val deaktivertVirksomhetsnummer2 = basePNarmesteLederRelasjon.copy(
                    id = secondInnmeldtVirksomhetsnummer2.id + 1,
                    referanseUuid = UUID.randomUUID(),
                    aktivTom = LocalDate.now().minusDays(10),
                    virksomhetsnummer = VIRKSOMHETSNUMMER_ALTERNATIVE,
                    timestamp = secondInnmeldtVirksomhetsnummer2.timestamp.plusDays(1),
                )
                val inputListVirksomhetsnummer2 = listOf(
                    firstInnmeldtVirksomhetsnummer2,
                    secondInnmeldtVirksomhetsnummer2,
                    deaktivertVirksomhetsnummer2,
                )
                val outputList = inputListVirksomhetsnummer2.plus(inputListVirksomhetsnummer1).toNarmesteLederRelasjonList()

                val outputListVirksomhetsnummer1 = outputList.filter {
                    it.virksomhetsnummer.value == VIRKSOMHETSNUMMER_DEFAULT.value
                }
                outputListVirksomhetsnummer1.size shouldBeEqualTo inputListVirksomhetsnummer1.size

                outputListVirksomhetsnummer1[0].id shouldBeEqualTo deaktivertVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[0].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT

                outputListVirksomhetsnummer1[1].id shouldBeEqualTo secondInnmeldtVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[1].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_INAKTIV

                outputListVirksomhetsnummer1[2].id shouldBeEqualTo firstInnmeldtVirksomhetsnummer1.id
                outputListVirksomhetsnummer1[2].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_INAKTIV

                val outputListVirksomhetsnummer2 = outputList.filter {
                    it.virksomhetsnummer.value == VIRKSOMHETSNUMMER_ALTERNATIVE.value
                }
                outputListVirksomhetsnummer2.size shouldBeEqualTo inputListVirksomhetsnummer2.size

                outputListVirksomhetsnummer2[0].id shouldBeEqualTo deaktivertVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[0].status shouldBeEqualTo NarmesteLederRelasjonStatus.DEAKTIVERT

                outputListVirksomhetsnummer2[1].id shouldBeEqualTo secondInnmeldtVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[1].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_INAKTIV

                outputListVirksomhetsnummer2[2].id shouldBeEqualTo firstInnmeldtVirksomhetsnummer2.id
                outputListVirksomhetsnummer2[2].status shouldBeEqualTo NarmesteLederRelasjonStatus.INNMELDT_INAKTIV
            }
        }
    }
})
