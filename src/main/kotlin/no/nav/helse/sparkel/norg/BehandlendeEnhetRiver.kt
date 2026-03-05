package no.nav.helse.sparkel.norg

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class BehandlendeEnhetRiver(
    rapidsConnection: RapidsConnection,
    private val personinfoService: PersoninfoService
) : River.PacketListener {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireAll("@behov", listOf("HentEnhet"))
                it.forbid("@løsning")
            }
            validate {
                it.requireKey("@id")
                it.requireKey("fødselsnummer")
                it.interestedIn("hendelseId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) = runBlocking {
        val meldingId = packet["@id"].asText()
        val godkjenningsbehovId = packet["hendelseId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        log.info(
            "Henter behandlende enhet for {}, {}",
            keyValue("hendelseId", godkjenningsbehovId),
            keyValue("@id", meldingId)
        )
        try {
            val enhet = withMDC("fødselsnummer" to fødselsnummer, "meldingId" to meldingId) {
                personinfoService.finnBehandlendeEnhet(fødselsnummer = fødselsnummer, callId = meldingId)
            }
            packet["@løsning"] = mapOf(
                "HentEnhet" to enhet
            )
            context.publish(packet.toJson())
        } catch (err: Exception) {
            log.error(
                "Feil ved håndtering av behov {} for {}: ${err.message}",
                keyValue("hendelseId", godkjenningsbehovId),
                keyValue("@id", meldingId),
                err
            )
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerLogg.error("Forstod ikke HentEnhet-behov:\n${problems.toExtendedReport()}")
    }
}

suspend fun <T> withMDC(vararg values: Pair<String, String>, block: suspend () -> T): T = try {
    values.forEach { (key, value) -> MDC.put(key, value) }
    block()
} finally {
    values.forEach { (key, _) -> MDC.remove(key) }
}
