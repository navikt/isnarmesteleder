package no.nav.syfo.client.pdl

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.azuread.AzureAdToken
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class PdlClient(
    private val azureAdClient: AzureAdClient,
    private val pdlClientId: String,
    private val pdlBaseUrl: String,
) {
    private val httpClient = httpClientDefault()

    suspend fun personIdentNumberNavnMap(
        callId: String,
        personIdentNumberList: List<PersonIdentNumber>,
    ): Map<String, String>? {
        if (personIdentNumberList.isEmpty()) {
            return emptyMap()
        }
        val token = azureAdClient.getSystemToken(pdlClientId)
            ?: throw RuntimeException("Failed to send request to PDL: No token was found")

        return personList(
            callId = callId,
            personIdentNumberList = personIdentNumberList,
            token = token,
        )?.hentPersonBolk?.associate { (ident, person) ->
            ident to (person?.fullName() ?: "")
        }
    }

    private suspend fun personList(
        callId: String,
        personIdentNumberList: List<PersonIdentNumber>,
        token: AzureAdToken,
    ): PdlHentPersonBolkData? {
        val query = this::class.java.getResource("/pdl/hentPersonBolk.graphql")
            .readText()
            .replace("[\n\r]", "")

        val request = PdlPersonBolkRequest(
            query = query,
            variables = PdlPersonBolkVariables(
                identer = personIdentNumberList.map { personIdentNumber ->
                    personIdentNumber.value
                }
            ),
        )

        val response: HttpResponse = httpClient.post(pdlBaseUrl) {
            body = request
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, bearerHeader(token.accessToken))
            header(TEMA_HEADER, ALLE_TEMA_HEADERVERDI)
            header(NAV_CALL_ID_HEADER, callId)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val pdlPersonReponse = response.receive<PdlPersonBolkResponse>()
                return if (!pdlPersonReponse.errors.isNullOrEmpty()) {
                    COUNT_CALL_PDL_PERSONBOLK_FAIL.increment()
                    pdlPersonReponse.errors.forEach {
                        logger.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                    }
                    null
                } else {
                    COUNT_CALL_PDL_PERSONBOLK_SUCCESS.increment()
                    pdlPersonReponse.data
                }
            }
            else -> {
                COUNT_CALL_PDL_PERSONBOLK_FAIL.increment()
                logger.error("Request with url: $pdlBaseUrl failed with reponse code ${response.status.value}")
                return null
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PdlClient::class.java)
    }
}
