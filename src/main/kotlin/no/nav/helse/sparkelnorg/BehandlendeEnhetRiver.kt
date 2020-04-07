package no.nav.helse.sparkelnorg

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

@KtorExperimentalAPI
class BehandlendeEnhetRiver(
    rapidsConnection: RapidsConnection,
    private val personinfoService: PersoninfoService
) : River.PacketListener {
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
}