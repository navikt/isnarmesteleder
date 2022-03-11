package testhelper.mock

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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
        false,
        ""
    )
    val tilgangTrue = Tilgang(
        true,
        ""
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
