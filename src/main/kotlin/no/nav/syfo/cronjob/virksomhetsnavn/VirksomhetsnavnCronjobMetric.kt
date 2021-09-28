package no.nav.syfo.cronjob.virksomhetsnavn

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CRONJOB_VIRKSOMHETSNAVN_BASE = "${METRICS_NS}_cronjob_virksomhetsnavn"
const val CRONJOB_VIRKSOMHETSNAVN_UPDATE = "${CRONJOB_VIRKSOMHETSNAVN_BASE}_update_count"
const val CRONJOB_VIRKSOMHETSNAVN_FAIL = "${CRONJOB_VIRKSOMHETSNAVN_BASE}_fail_count"

val COUNT_CRONJOB_VIRKSOMHETSNAVN_UPDATE: Counter = Counter
    .builder(CRONJOB_VIRKSOMHETSNAVN_UPDATE)
    .description("Counts the number of updates in Cronjob - VirksomhetsnavnCronjob")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_VIRKSOMHETSNAVN_FAIL: Counter = Counter
    .builder(CRONJOB_VIRKSOMHETSNAVN_FAIL)
    .description("Counts the number of failures in Cronjob - VirksomhetsnavnCronjob")
    .register(METRICS_REGISTRY)
