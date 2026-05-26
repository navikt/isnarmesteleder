package testhelper

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.narmestelederrelasjon.NarmesteLederRelasjonService

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val azureAdClient = AzureAdClient(
        azureEnviroment = externalMockEnvironment.environment.azure,
        valkeyStore = externalMockEnvironment.cache,
        httpClient = externalMockEnvironment.mockHttpClient,
    )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        wellKnownSelvbetjening = externalMockEnvironment.wellKnownSelvbetjening,
        veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
            azureAdClient = azureAdClient,
            clientEnvironment = externalMockEnvironment.environment.clients.tilgangskontroll,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
        narmesteLederRelasjonService = NarmesteLederRelasjonService(
            database = externalMockEnvironment.database,
            eregClient = EregClient(
                clientEnvironment = externalMockEnvironment.environment.clients.ereg,
                valkeyStore = externalMockEnvironment.cache,
                httpClient = externalMockEnvironment.mockHttpClient,
            ),
            pdlClient = PdlClient(
                azureAdClient = azureAdClient,
                clientEnvironment = externalMockEnvironment.environment.clients.pdl,
                valkeyStore = externalMockEnvironment.cache,
                httpClient = externalMockEnvironment.mockHttpClient,
            ),
        )
    )
}
