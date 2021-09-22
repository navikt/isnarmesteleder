package no.nav.syfo.application.cronjob

import io.ktor.application.*
import no.nav.syfo.application.Environment
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.ereg.EregClient

fun Application.cronjobModule(
    environment: Environment,
) {
    val azureAdClient = AzureAdClient(
        azureAppClientId = environment.azureAppClientId,
        azureAppClientSecret = environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
    )

    val eregClient = EregClient(
        azureAdClient = azureAdClient,
        isproxyClientId = environment.isproxyClientId,
        baseUrl = environment.isproxyUrl,
    )
}
