package no.nav.helse.sparkelnorg

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
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
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    init {
        River(rapidsConnection).apply {
            validate { it.requireAny("@behov", listOf("HentEnhet")) }
            validate { it.requireKey("fødselsnummer") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) = runBlocking {
        val enhet = personinfoService.finnBehandlendeEnhet(packet["fødselsnummer"].asText())
        packet["@løsning"] = mapOf(
            "HentEnhet" to enhet
        )
        context.send(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.info(problems.toExtendedReport())
    }
}