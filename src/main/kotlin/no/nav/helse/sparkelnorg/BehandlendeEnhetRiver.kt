package no.nav.helse.sparkelnorg

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
class BehandlendeEnhetRiver(
    rapidsConnection: RapidsConnection,
    private val personinfoService: PersoninfoService
) : River.PacketListener {
    private val log: Logger = LoggerFactory.getLogger("hent-navn")
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    init {
        River(rapidsConnection).apply {
            validate {
                it.requireAll("@behov", listOf("HentEnhet"))
                it.forbid("@løsning")
            }
            validate { it.requireKey("fødselsnummer", "spleisBehovId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) = runBlocking {
        val enhet = personinfoService.finnBehandlendeEnhet(packet["fødselsnummer"].asText())
        packet["@løsning"] = mapOf(
            "HentEnhet" to enhet
        )
        log.info("Henter behandlende enhet for {}", keyValue("spleisBehovId", packet["spleisBehovId"].asText()))
        context.send(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.debug(problems.toExtendedReport())
    }
}