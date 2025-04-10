package no.nav.syfo.application.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.client.wellknown.WellKnown
import no.nav.syfo.narmestelederrelasjon.NarmesteLederRelasjonService
import no.nav.syfo.narmestelederrelasjon.api.*
import no.nav.syfo.narmestelederrelasjon.api.access.APIConsumerAccessService

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownInternalAzureAD: WellKnown,
    wellKnownSelvbetjening: WellKnown,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    narmesteLederRelasjonService: NarmesteLederRelasjonService,
) {
    installMetrics()
    installCallId()
    installContentNegotiation()
    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.azure.appClientId),
                jwtIssuerType = JwtIssuerType.INTERNAL_AZUREAD,
                wellKnown = wellKnownInternalAzureAD,
            ),
            JwtIssuer(
                acceptedAudienceList = listOf(environment.tokenx.tokenxClientId),
                jwtIssuerType = JwtIssuerType.SELVBETJENING,
                wellKnown = wellKnownSelvbetjening,
            ),
        ),
    )
    installStatusPages()

    val apiConsumerAccessService = APIConsumerAccessService(
        azureAppPreAuthorizedApps = environment.azure.appPreAuthorizedApps,
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerPrometheusApi()
        authenticate(JwtIssuerType.INTERNAL_AZUREAD.name) {
            registrerNarmesteLederRelasjonApi(
                narmesteLederRelasjonService = narmesteLederRelasjonService,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            )
            registrerNarmesteLederRelasjonSystemApi(
                apiConsumerAccessService = apiConsumerAccessService,
                authorizedApplicationNameList = environment.systemAPIAuthorizedConsumerApplicationNameList,
                narmesteLederRelasjonService = narmesteLederRelasjonService,
            )
        }
        authenticate(JwtIssuerType.SELVBETJENING.name) {
            registrerNarmesteLederRelasjonSelvbetjeningApi(
                narmesteLederRelasjonService = narmesteLederRelasjonService,
            )
        }
    }
}
