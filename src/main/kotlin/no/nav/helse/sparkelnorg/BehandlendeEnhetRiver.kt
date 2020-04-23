package no.nav.helse.sparkelnorg

import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BehandlendeEnhetRiver(
    rapidsConnection: RapidsConnection,
    private val personinfoService: PersoninfoService
) : River.PacketListener {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("HentEnhet"))
                it.rejectKey("@løsning")
                it.requireKey("fødselsnummer", "spleisBehovId", "vedtaksperiodeId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) = runBlocking {
        log.info(
            "Henter behandlende enhet for {}, {}",
            keyValue("spleisBehovId", packet["spleisBehovId"].asText()),
            keyValue("vedtaksperiodeId", packet["vedtaksperiodeId"].asText())
        )
        try {
            val enhet = personinfoService.finnBehandlendeEnhet(packet["fødselsnummer"].asText())
            packet["@løsning"] = mapOf(
                "HentEnhet" to enhet
            )
            context.send(packet.toJson())
        } catch (err: Exception) {
            log.error("feil ved håntering av behov {} for {}: ${err.message}",
                keyValue("spleisBehovId", packet["spleisBehovId"].asText()),
                keyValue("vedtaksperiodeId", packet["vedtaksperiodeId"].asText()),
                err)
        }
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.error("Forstod ikke HentEnhet-behov:\n${problems.toExtendedReport()}")
    }
}
