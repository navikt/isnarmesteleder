package no.nav.syfo.cronjob

import no.nav.syfo.application.*
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.VirksomhetsnavnService

fun cronjobModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    redisStore: RedisStore,
) {
    val eregClient = EregClient(
        clientEnvironment = environment.clients.ereg,
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
