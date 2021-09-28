package no.nav.syfo.cronjob.virksomhetsnavn

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.cronjob.Cronjob
import no.nav.syfo.cronjob.CronjobResult
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.ereg.virksomhetsnavn
import org.slf4j.LoggerFactory
import java.util.*

class VirksomhetsnavnCronjob(
    private val eregClient: EregClient,
    private val virksomhetsnavnService: VirksomhetsnavnService,
) : Cronjob {
    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 60

    override suspend fun run() {
        virksomhetsnavnJob()
    }

    suspend fun virksomhetsnavnJob(): CronjobResult {
        val virksomhetsnavnResult = CronjobResult()

        val narmesteLederRelasjonList = virksomhetsnavnService.getNarmesteLederRelasjonWithoutVirksomhetsnavnList()
        narmesteLederRelasjonList.forEach { narmesteLederRelasjon ->
            try {
                eregClient.organisasjon(
                    callId = UUID.randomUUID().toString(),
                    virksomhetsnummer = narmesteLederRelasjon.virksomhetsnummer,
                )
                    ?.virksomhetsnavn()
                    ?.let { virksomhetsnavn ->
                        virksomhetsnavnService.updateVirksomhetsnavn(
                            narmesteLederRelasjonId = narmesteLederRelasjon.id,
                            virksomhetsnavn = virksomhetsnavn,
                        )
                        virksomhetsnavnResult.updated++
                        COUNT_CRONJOB_VIRKSOMHETSNAVN_UPDATE.increment()
                    } ?: throw RuntimeException("Failed to store Virksomhetsnavn: response from Ereg missing")
            } catch (e: Exception) {
                log.error("Exception caught while attempting store Virksomhetsnavn for NarmesteLederRelasjon", e)
                virksomhetsnavnResult.failed++
                COUNT_CRONJOB_VIRKSOMHETSNAVN_FAIL.increment()
            }
        }
        log.info(
            "Completed Virksomhetsnavn-cronjob with result: {}, {}",
            StructuredArguments.keyValue("failed", virksomhetsnavnResult.failed),
            StructuredArguments.keyValue("updated", virksomhetsnavnResult.updated),
        )
        return virksomhetsnavnResult
    }

    companion object {
        private val log = LoggerFactory.getLogger(VirksomhetsnavnCronjob::class.java)
    }
}
