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
class HentNavnRiver(
    rapidsConnection: RapidsConnection,
    private val personinfoService: PersoninfoService
) : River.PacketListener {
    private val log: Logger = LoggerFactory.getLogger("hent-navn")
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate { it.requireAny("@behov", listOf("HentEnhet")) }
            validate { it.requireKey("fødselsnummer", "spleisBehovId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) = runBlocking {
        val fnr = packet["fødselsnummer"].asText()
        log.info("Henter personinfo for {}", keyValue("spleisBehovId", packet["spleisBehovId"].asText()))
        val person = personinfoService.finnPerson(fnr) ?: return@runBlocking

        packet["@løsning"] = mapOf(
            "HentNavn" to mapOf(
                "fornavn" to person.personnavn.fornavn,
                "mellomnavn" to person.personnavn.mellomnavn,
                "etternavn" to person.personnavn.etternavn
            )
        )
        context.send(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.info(problems.toExtendedReport())
    }
}