package no.nav.syfo.cronjob

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnService
import no.nav.syfo.narmestelederrelasjon.kafka.launchBackgroundTask

fun cronjobModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
) {
    val redisStore = RedisStore(
        redisEnvironment = environment.redis,
    )

    val azureAdClient = AzureAdClient(
        azureEnviroment = environment.azure,
        redisStore = redisStore,
    )

    val eregClient = EregClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.isproxy,
        redisStore = redisStore,
    )

    val leaderPodClient = LeaderPodClient(
        electorPath = environment.electorPath,
    )

    val virksomhetsnavnService = VirksomhetsnavnService(
        database = database,
    )

    val virksomhetsnavnCronjob = VirksomhetsnavnCronjob(
        eregClient = eregClient,
        virksomhetsnavnService = virksomhetsnavnService,
    )

    val cronjobRunner = CronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient
    )

    launchBackgroundTask(
        applicationState = applicationState,
    ) {
        cronjobRunner.start(
            cronjob = virksomhetsnavnCronjob,
        )
    }
}
