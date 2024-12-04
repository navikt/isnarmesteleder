package testhelper.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.client.ereg.EregOrganisasjonNavn
import no.nav.syfo.client.ereg.EregOrganisasjonResponse
import testhelper.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import testhelper.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN

val eregOrganisasjonMockResponse = EregOrganisasjonResponse(
    navn = EregOrganisasjonNavn(
        navnelinje1 = "Virksom Bedrift AS",
        redigertnavn = "Virksom Bedrift AS, Norge",
    )
)

fun MockRequestHandleScope.eregMockResponse(request: HttpRequestData): HttpResponseData {
    val requestUrl = request.url.encodedPath

    return when {
        requestUrl.endsWith(VIRKSOMHETSNUMMER_DEFAULT.value) -> respondOk(eregOrganisasjonMockResponse)
        requestUrl.endsWith(VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN.value) -> respondError(status = HttpStatusCode.InternalServerError)
        else -> error("Unhandled path $requestUrl")
    }
}
