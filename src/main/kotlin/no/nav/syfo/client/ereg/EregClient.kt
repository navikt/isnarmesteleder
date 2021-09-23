package no.nav.syfo.client.ereg

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class EregClient(
    private val azureAdClient: AzureAdClient,
    private val isproxyClientId: String,
    baseUrl: String,
) {
    private val httpClient = httpClientDefault()

    private val eregOrganisasjonUrl: String = "$baseUrl$EREG_PATH"

    suspend fun organisasjon(
        callId: String,
        orgNr: String,
    ): EregOrganisasjonResponse? {
        val systemToken = azureAdClient.getSystemToken(
            scopeClientId = isproxyClientId,
        )?.accessToken
            ?: throw RuntimeException("Failed to request Organisasjon from Isproxy-Ereg: Failed to get system token from AzureAD")

        try {
            val url = "$eregOrganisasjonUrl/$orgNr"
            val response: EregOrganisasjonResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(systemToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_EREG_ORGANISASJON_SUCCESS.increment()
            return response
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e.response, e.message, callId)
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e.response, e.message, callId)
        }
        return null
    }

    private fun handleUnexpectedResponseException(
        response: HttpResponse,
        message: String?,
        callId: String,
    ) {
        log.error(
            "Error while requesting Response from Ereg {}, {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            StructuredArguments.keyValue("message", message),
            StructuredArguments.keyValue("callId", callId),
        )
        COUNT_CALL_EREG_ORGANISASJON_FAIL.increment()
    }

    companion object {
        const val EREG_PATH = "/api/v1/ereg/organisasjon"
        private val log = LoggerFactory.getLogger(EregClient::class.java)
    }
}
