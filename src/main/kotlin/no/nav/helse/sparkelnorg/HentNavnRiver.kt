package no.nav.helse.sparkelnorg

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

@KtorExperimentalAPI
class HentNavnRiver(
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
        val person = personinfoService.finnPerson(packet["fødselsnummer"].asText()) ?: return@runBlocking

        packet["@løsning"] = mapOf(
            "HentNavn" to mapOf(
                "fornavn" to person.personnavn.fornavn,
                "mellomnavn" to person.personnavn.mellomnavn,
                "etternavn" to person.personnavn.etternavn
            )
        )
        context.send(packet.toJson())
    }
}