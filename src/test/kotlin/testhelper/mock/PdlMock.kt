package testhelper.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.pdl.PdlClient.Companion.IDENTER_HEADER
import no.nav.syfo.client.pdl.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import testhelper.UserConstants
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER

fun PersonIdentNumber.toHistoricalPersonIdentNumber(): PersonIdentNumber {
    val firstDigit = this.value[0].digitToInt()
    val newDigit = firstDigit + 4
    val dNummer = this.value.replace(
        firstDigit.toString(),
        newDigit.toString(),
    )
    return PersonIdentNumber(dNummer)
}

fun generatePdlIdenterResponse(
    personIdentNumber: PersonIdentNumber,
) = PdlIdenterResponse(
    data = PdlHentIdenter(
        hentIdenter = PdlIdenter(
            identer = listOf(
                PdlIdent(
                    ident = personIdentNumber.value,
                    historisk = false,
                    gruppe = IdentType.FOLKEREGISTERIDENT.name,
                ),
                PdlIdent(
                    ident = personIdentNumber.toHistoricalPersonIdentNumber().value,
                    historisk = true,
                    gruppe = IdentType.FOLKEREGISTERIDENT.name,
                ),
            ),
        ),
    ),
    errors = null,
)

fun generatePdlPersonResponse() =
    PdlPersonBolkResponse(
        errors = null,
        data = generatePdlHentPersonBolkData(),
    )

fun generatePdlHentPersonBolkData() =
    PdlHentPersonBolkData(
        hentPersonBolk = listOf(
            generatePdlHentPerson(),
        ),
    )

fun generatePdlHentPerson(
    pdlPersonNavn: PdlPersonNavn = generatePdlPersonNavn(),
) = PdlHentPerson(
    ident = NARMESTELEDER_PERSONIDENTNUMBER.value,
    person = generatePdlPerson(pdlPersonNavn = pdlPersonNavn),
    code = "ok",
)

fun generatePdlPerson(
    pdlPersonNavn: PdlPersonNavn = generatePdlPersonNavn(),
) = PdlPerson(
    navn = listOf(
        pdlPersonNavn,
    ),
)

fun generatePdlPersonNavn() =
    PdlPersonNavn(
        fornavn = UserConstants.NARMESTELEDER_FORNAVN,
        mellomnavn = UserConstants.NARMESTELEDER_MELLOMNAVN,
        etternavn = UserConstants.NARMESTELEDER_ETTERNAVN,
    )

val pdlPersonMockRespons = generatePdlPersonResponse()

suspend fun MockRequestHandleScope.pdlMockResponse(request: HttpRequestData): HttpResponseData =
    if (request.headers[IDENTER_HEADER] == IDENTER_HEADER) {
        val pdlRequest = request.receiveBody<PdlHentIdenterRequest>()
        val personIdentNumber = PersonIdentNumber(pdlRequest.variables.ident)
        val response = generatePdlIdenterResponse(
            personIdentNumber = personIdentNumber,
        )
        respondOk(response)
    } else {
        respondOk(pdlPersonMockRespons)
    }
