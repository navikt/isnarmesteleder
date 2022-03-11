package testhelper.mock

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.client.ereg.EregClient.Companion.EREG_PATH
import no.nav.syfo.client.ereg.EregOrganisasjonNavn
import no.nav.syfo.client.ereg.EregOrganisasjonResponse
import testhelper.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import testhelper.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN
import testhelper.getRandomPort

class IsproxyMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val eregOrganisasjonResponse = EregOrganisasjonResponse(
        navn = EregOrganisasjonNavn(
            navnelinje1 = "Virksom Bedrift AS",
            redigertnavn = "Virksom Bedrift AS, Norge",
        )
    )

    val name = "isproxy"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get("$EREG_PATH/${VIRKSOMHETSNUMMER_DEFAULT.value}") {
                call.respond(eregOrganisasjonResponse)
            }
            get("$EREG_PATH/${VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN.value}") {
                call.respond(HttpStatusCode.InternalServerError, "")
            }
        }
    }
}
