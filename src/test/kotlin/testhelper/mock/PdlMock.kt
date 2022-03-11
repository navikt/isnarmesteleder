package testhelper.mock

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.client.pdl.PdlClient.Companion.IDENTER_HEADER
import no.nav.syfo.client.pdl.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import testhelper.UserConstants
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.getRandomPort

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

class PdlMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val name = "pdl"

    val respons = generatePdlPersonResponse()

    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            post {
                if (call.request.headers[IDENTER_HEADER] == IDENTER_HEADER) {
                    val pdlRequest = call.receive<PdlHentIdenterRequest>()
                    val personIdentNumber = PersonIdentNumber(pdlRequest.variables.ident)
                    val response = generatePdlIdenterResponse(
                        personIdentNumber = personIdentNumber,
                    )
                    call.respond(response)
                } else {
                    val pdlRequest = call.receive<PdlPersonBolkRequest>()
                    if (pdlRequest.variables.identer.contains(NARMESTELEDER_PERSONIDENTNUMBER.value)) {
                        call.respond(respons)
                    }
                }
            }
        }
    }
}
