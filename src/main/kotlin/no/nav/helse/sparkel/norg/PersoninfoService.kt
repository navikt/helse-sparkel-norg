package no.nav.helse.sparkel.norg

import com.github.navikt.tbd_libs.result_object.getOrThrow
import com.github.navikt.tbd_libs.speed.GeografiskTilknytningResponse
import com.github.navikt.tbd_libs.speed.PersonResponse
import com.github.navikt.tbd_libs.speed.SpeedClient
import no.nav.helse.sparkel.retry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

class PersoninfoService(private val norg2Client: Norg2Client, private val speedClient: SpeedClient) {

    suspend fun finnBehandlendeEnhet(fødselsnummer: String, callId: String): String {
        val adresseBeskyttelse = finnAdressebeskyttelse(fødselsnummer, callId).norgkode
        val geografiskTilknytning = finnGeografiskTilknytning(fødselsnummer, callId)
        val geografiskOmraade = geografiskTilknytning.mestNøyaktig()
        sikkerlogg.info("Geografisk tilknytning: $geografiskTilknytning - spør NORG2 om behandlende enhet for $geografiskOmraade")
        val behandlendeEnhet = norg2Client.finnBehandlendeEnhet(geografiskOmraade, adresseBeskyttelse)
        sikkerlogg.info("Fant behandlende enhet for fødselsnummer {}: $behandlendeEnhet", MDC.get("fødselsnummer"))
        return behandlendeEnhet.enhetNr
    }

    private suspend fun finnAdressebeskyttelse(fødselsnummer: String, callId: String): PersonResponse.Adressebeskyttelse = retry(
        "pdl_hent_person", retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L)
    ) {
        speedClient.hentPersoninfo(fødselsnummer, callId).getOrThrow().adressebeskyttelse
    }

    private suspend fun finnGeografiskTilknytning(fødselsnummer: String, behovId: String): GeografiskTilknytningResponse = retry(
        "pdl_hent_geografisktilknytning", retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L)
    ) {
        speedClient.hentGeografiskTilknytning(fødselsnummer, behovId).getOrThrow()
    }

    private fun GeografiskTilknytningResponse.mestNøyaktig() = bydel ?: kommune ?: land ?: "ukjent"
    private val PersonResponse.Adressebeskyttelse.norgkode
        get() = when (this) {
            PersonResponse.Adressebeskyttelse.FORTROLIG -> "SPFO"
            PersonResponse.Adressebeskyttelse.STRENGT_FORTROLIG -> "SPSF"
            PersonResponse.Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND -> "SPSF"
            PersonResponse.Adressebeskyttelse.UGRADERT -> ""
        }
}
