package no.nav.helse.sparkelnorg

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class BehandlendeEnhetRiver(
    rapidsConnection: RapidsConnection,
    private val behandlendeEnhetService: BehandlendeEnhetService
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.requireAny("@behov", listOf("HentEnhet")) }
            validate { it.requireKey("fødselsnummer") }
        }
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) = runBlocking {
        val enhet = behandlendeEnhetService.finnBehandlendeEnhet(packet["fødselsnummer"].asText())
        packet["@løsning"] = mapOf(
            "HentEnhet" to enhet
        )
        context.send(packet.toJson())
    }
}