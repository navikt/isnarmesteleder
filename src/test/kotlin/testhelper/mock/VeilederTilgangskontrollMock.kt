package testhelper.mock

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient.Companion.TILGANGSKONTROLL_PERSON_PATH
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import testhelper.getRandomPort

class VeilederTilgangskontrollMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val tilgangFalse = Tilgang(
        harTilgang = false,
    )
    val tilgangTrue = Tilgang(
        harTilgang = true,
    )

    val name = "veiledertilgangskontroll"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get(TILGANGSKONTROLL_PERSON_PATH) {
                when {
                    call.request.headers[NAV_PERSONIDENT_HEADER] == ARBEIDSTAKER_VEILEDER_NO_ACCESS.value -> {
                        call.respond(HttpStatusCode.Forbidden, tilgangFalse)
                    }
                    call.request.headers[NAV_PERSONIDENT_HEADER] != null -> {
                        call.respond(tilgangTrue)
                    }
                    else -> {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
