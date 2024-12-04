package testhelper.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS

fun MockRequestHandleScope.tilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    val personident = request.headers[NAV_PERSONIDENT_HEADER]

    return when {
        personident == ARBEIDSTAKER_VEILEDER_NO_ACCESS.value -> respondOk(body = Tilgang(erGodkjent = false))
        personident != null -> respondOk(Tilgang(erGodkjent = true))
        else -> respondError(HttpStatusCode.BadRequest)
    }
}
