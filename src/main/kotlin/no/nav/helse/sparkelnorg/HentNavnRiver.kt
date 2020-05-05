package no.nav.helse.sparkelnorg

import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HentNavnRiver(
    rapidsConnection: RapidsConnection,
    private val personinfoService: PersoninfoService
) : River.PacketListener {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("HentPersoninfo"))
                it.rejectKey("@løsning")
                it.requireKey("fødselsnummer", "spleisBehovId", "vedtaksperiodeId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) = runBlocking {
        val fnr = packet["fødselsnummer"].asText()
        log.info(
            "Henter personinfo for {}, {}",
            keyValue(
                "spleisBehovId",
                packet["spleisBehovId"].asText()
            ),
            keyValue("spleisBehovId", packet["spleisBehovId"].asText()),
            keyValue("vedtaksperiodeId", packet["vedtaksperiodeId"].asText())
        )
        try {
            val person = personinfoService.finnPerson(fnr) ?: return@runBlocking
            packet["@løsning"] = mapOf(
                "HentPersoninfo" to mapOf(
                    "fornavn" to person.personnavn.fornavn,
                    "mellomnavn" to person.personnavn.mellomnavn,
                    "etternavn" to person.personnavn.etternavn,
                    "fødselsdato" to person.foedselsdato.foedselsdato.toGregorianCalendar().toZonedDateTime().toLocalDate(),
                    "kjønn" to person.kjoenn.tilKjønn()
                )
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
        sikkerLogg.error("Forstod ikke HentPersoninfo-behov:\n${problems.toExtendedReport()}")
    }
}

private fun Kjoenn.tilKjønn() = when(this.kjoenn.value) {
    "K" -> Kjønn.Kvinne
    "M" -> Kjønn.Mann
    else -> Kjønn.Ukjent
}

enum class Kjønn {
    Mann, Kvinne, Ukjent
}
