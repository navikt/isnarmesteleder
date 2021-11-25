package no.nav.syfo.client.pdl

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.azuread.AzureAdToken
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.pdl.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class PdlClient(
    private val azureAdClient: AzureAdClient,
    private val pdlClientId: String,
    private val pdlBaseUrl: String,
    private val redisStore: RedisStore,
) {
    private val httpClient = httpClientDefault()

    suspend fun identList(
        callId: String,
        withHistory: Boolean,
        personIdentNumber: PersonIdentNumber,
    ): List<PersonIdentNumber>? {
        val cacheKey = personIdentIdenterCacheKey(
            personIdentNumber = personIdentNumber,
        )
        val cachedValue: PdlPersonidentIdenterCache? = redisStore.getObject(key = cacheKey)
        if (cachedValue != null) {
            COUNT_CALL_PDL_IDENTER_CACHE_HIT.increment()
            return cachedValue.personIdentList.map { cachedPersonIdent ->
                PersonIdentNumber(cachedPersonIdent)
            }
        } else {
            COUNT_CALL_PDL_IDENTER_CACHE_MISS.increment()
            return pdlIdenter(
                callId = callId,
                withHistory = withHistory,
                personIdentNumber = personIdentNumber,
            )?.hentIdenter?.let { identer ->
                redisStore.setObject(
                    key = cacheKey,
                    value = PdlPersonidentIdenterCache(
                        personIdentList = identer.identer.map { it.ident }
                    ),
                    expireSeconds = CACHE_PDL_PERSONIDENT_IDENTER_TIME_TO_LIVE_SECONDS
                )
                identer.toPersonIdentNumberList()
            }
        }
    }

    private suspend fun pdlIdenter(
        callId: String,
        withHistory: Boolean,
        personIdentNumber: PersonIdentNumber,
    ): PdlHentIdenter? {
        val token = azureAdClient.getSystemToken(pdlClientId)
            ?: throw RuntimeException("Failed to send PdlHentIdenterRequest to PDL: No token was found")

        val query = getPdlQuery(
            queryFilePath = "/pdl/hentIdenter.graphql",
        )

        val request = PdlHentIdenterRequest(
            query = query,
            variables = PdlHentIdenterRequestVariables(
                ident = personIdentNumber.value,
                historikk = withHistory,
                grupper = listOf(
                    IdentType.FOLKEREGISTERIDENT.name,
                ),
            ),
        )

        val response: HttpResponse = httpClient.post(pdlBaseUrl) {
            body = request
            header(HttpHeaders.Authorization, bearerHeader(token.accessToken))
            header(HttpHeaders.ContentType, APPLICATION_JSON)
            header(TEMA_HEADER, ALLE_TEMA_HEADERVERDI)
            header(NAV_CALL_ID_HEADER, callId)
            header(IDENTER_HEADER, IDENTER_HEADER)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val pdlIdenterResponse = response.receive<PdlIdenterResponse>()
                return if (!pdlIdenterResponse.errors.isNullOrEmpty()) {
                    COUNT_CALL_PDL_PERSONBOLK_FAIL.increment()
                    pdlIdenterResponse.errors.forEach {
                        logger.error("Error while requesting IdentList from PersonDataLosningen: ${it.errorMessage()}")
                    }
                    null
                } else {
                    COUNT_CALL_PDL_IDENTER_SUCCESS.increment()
                    pdlIdenterResponse.data
                }
            }
            else -> {
                COUNT_CALL_PDL_IDENTER_FAIL.increment()
                logger.error("Request to get IdentList with url: $pdlBaseUrl failed with reponse code ${response.status.value}")
                return null
            }
        }
    }

    private fun personIdentIdenterCacheKey(personIdentNumber: PersonIdentNumber) =
        "$CACHE_PDL_PERSONIDENT_IDENTER_KEY_PREFIX${personIdentNumber.value}"

    suspend fun personIdentNumberNavnMap(
        callId: String,
        personIdentNumberList: List<PersonIdentNumber>,
    ): Map<String, String> {
        val cachedPersonIdentNameMap = getCachedPersonidentNameMap(
            personIdentNumberList = personIdentNumberList,
        )

        val notCachedPersonIdentNumberList = personIdentNumberList.filterNot { personIdentNumber ->
            cachedPersonIdentNameMap.containsKey(personIdentNumber.value)
        }

        return if (notCachedPersonIdentNumberList.isEmpty()) {
            cachedPersonIdentNameMap
        } else {
            val pdlPersonIdentNameMap = getPdlPersonIdentNumberNavnMap(
                callId = callId,
                personIdentNumberList = notCachedPersonIdentNumberList,
            )
            cachedPersonIdentNameMap + pdlPersonIdentNameMap
        }
    }

    private suspend fun getPdlPersonIdentNumberNavnMap(
        callId: String,
        personIdentNumberList: List<PersonIdentNumber>,
    ): Map<String, String> {
        val token = azureAdClient.getSystemToken(pdlClientId)
            ?: throw RuntimeException("Failed to send request to PDL: No token was found")

        val pdlPersonIdentNameMap = personList(
            callId = callId,
            personIdentNumberList = personIdentNumberList,
            token = token,
        )?.hentPersonBolk?.associate { (ident, person) ->
            ident to (person?.fullName() ?: "")
        }

        pdlPersonIdentNameMap?.let {
            setPersonidentNameMap(pdlPersonIdentNameMap)
        }
        return pdlPersonIdentNameMap ?: emptyMap()
    }

    private fun getCachedPersonidentNameMap(
        personIdentNumberList: List<PersonIdentNumber>
    ): Map<String, String> {
        val cachedList = redisStore.getObjectList(
            classType = PdlPersonidentNameCache::class,
            keyList = personIdentNumberList.map { personIdentNumber ->
                personIdentNameCacheKey(personIdentNumber.value)
            },
        )
        if (cachedList.isEmpty()) {
            return emptyMap()
        }
        return cachedList.associate { pdlPersonIdentNameCache ->
            pdlPersonIdentNameCache.personIdent to (pdlPersonIdentNameCache.name)
        }
    }

    private fun setPersonidentNameMap(personIdentNameMap: Map<String, String>) {
        personIdentNameMap.forEach { personIdentName ->
            redisStore.setObject(
                key = personIdentNameCacheKey(personIdentName.key),
                value = PdlPersonidentNameCache(
                    name = personIdentName.value,
                    personIdent = personIdentName.key,
                ),
                expireSeconds = CACHE_PDL_PERSONIDENT_NAME_TIME_TO_LIVE_SECONDS,
            )
        }
    }

    private suspend fun personList(
        callId: String,
        personIdentNumberList: List<PersonIdentNumber>,
        token: AzureAdToken,
    ): PdlHentPersonBolkData? {
        val query = getPdlQuery(
            queryFilePath = "/pdl/hentPersonBolk.graphql",
        )

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
            header(HttpHeaders.Authorization, bearerHeader(token.accessToken))
            header(HttpHeaders.ContentType, APPLICATION_JSON)
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

    private fun personIdentNameCacheKey(personIdentNumber: String) =
        "$CACHE_PDL_PERSONIDENT_NAME_KEY_PREFIX$personIdentNumber"

    private fun getPdlQuery(queryFilePath: String): String {
        return this::class.java.getResource(queryFilePath)!!
            .readText()
            .replace("[\n\r]", "")
    }

    companion object {
        const val CACHE_PDL_PERSONIDENT_NAME_KEY_PREFIX = "pdl-personident-name-"
        const val CACHE_PDL_PERSONIDENT_NAME_TIME_TO_LIVE_SECONDS = 24 * 60 * 60L
        const val CACHE_PDL_PERSONIDENT_IDENTER_KEY_PREFIX = "pdl-personident-identer-"
        const val CACHE_PDL_PERSONIDENT_IDENTER_TIME_TO_LIVE_SECONDS = 12 * 60 * 60L

        const val IDENTER_HEADER = "identer"

        private val logger = LoggerFactory.getLogger(PdlClient::class.java)
    }
}
