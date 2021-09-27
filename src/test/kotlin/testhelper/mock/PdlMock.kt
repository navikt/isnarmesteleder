package testhelper.mock

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.client.pdl.*
import testhelper.UserConstants
import testhelper.UserConstants.NARMESTELEDER_PERSONIDENTNUMBER
import testhelper.getRandomPort

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
    val server = mockPdlServer()

    val respons = generatePdlPersonResponse()

    private fun mockPdlServer(): NettyApplicationEngine {
        return embeddedServer(
            factory = Netty,
            port = port
        ) {
            installContentNegotiation()
            routing {
                post {
                    val pdlRequest = call.receive<PdlPersonBolkRequest>()
                    if (pdlRequest.variables.identer.contains(NARMESTELEDER_PERSONIDENTNUMBER.value)) {
                        call.respond(respons)
                    }
                }
            }
        }
    }
}
