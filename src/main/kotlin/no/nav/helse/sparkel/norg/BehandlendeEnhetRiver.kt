package no.nav.helse.sparkel.norg

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking

class BehandlendeEnhetRiver(
    rapidsConnection: RapidsConnection,
    private val personinfoService: PersoninfoService
) : River.PacketListener {
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

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val meldingId = packet["@id"].asText()
        val hendelseId = packet["hendelseId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        medMdc(
            MdcKey.MELDING_ID to meldingId,
            MdcKey.HENDELSE_ID to hendelseId,
            MdcKey.IDENTITETSNUMMER to fødselsnummer,
        ) {
            loggInfo("Henter behandlende enhet")
            try {
                val enhet = runBlocking { personinfoService.finnBehandlendeEnhetsNr(fødselsnummer = fødselsnummer, callId = meldingId) }
                packet["@løsning"] = mapOf("HentEnhet" to enhet)
                context.publish(packet.toJson())
            } catch (err: Exception) {
                loggError("Feil ved håndtering av behov", err)
            }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        loggError("Forstod ikke HentEnhet-behov", "extendedReport" to problems.toExtendedReport())
    }
}
