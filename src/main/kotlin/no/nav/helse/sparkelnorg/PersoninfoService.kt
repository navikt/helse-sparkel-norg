package no.nav.helse.sparkelnorg

import com.ctc.wstx.exc.WstxException
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import java.io.IOException

class PersoninfoService(private val norg2Client: Norg2Client, private val personV3: PersonV3) {
    suspend fun finnBehandlendeEnhet(fødselsnummer: String): String {
        val diskresjonskode = requireNotNull(finnPerson(fødselsnummer)).diskresjonskode?.value
        val geografiskTilknytning =
            requireNotNull(finnGeografiskTilknytning(fødselsnummer)?.geografiskTilknytning?.geografiskTilknytning)
        return norg2Client.finnBehandlendeEnhet(geografiskTilknytning, diskresjonskode).enhetNr
    }

    internal suspend fun finnPerson(fødselsnummer: String): Person? = try {
        retry(
            "tps_hent_person",
            IOException::class, WstxException::class, IllegalStateException::class,
            retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L)
        ) {
            personV3.hentPerson(
                HentPersonRequest().withAktoer(
                    PersonIdent().withIdent(
                        NorskIdent()
                            .withIdent(fødselsnummer)
                            .withType(Personidenter().withValue("FNR"))
                    )
                )
            ).person
        }
    } catch (hentPersonPersonIkkeFunnet: HentPersonPersonIkkeFunnet) {
        null
    }

    private suspend fun finnGeografiskTilknytning(fødselsnummer: String): HentGeografiskTilknytningResponse? = try {
        retry(
            "tps_hent_geografisktilknytning",
            IOException::class, WstxException::class, IllegalStateException::class,
            IOException::class, WstxException::class, IllegalStateException::class,
            retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L)
        ) {
            personV3.hentGeografiskTilknytning(
                HentGeografiskTilknytningRequest().withAktoer(
                    PersonIdent().withIdent(
                        NorskIdent()
                            .withIdent(fødselsnummer)
                            .withType(Personidenter().withValue("FNR"))
                    )
                )
            )
        }
    } catch (hentGeografiskTilknytningPersonIkkeFunnet: HentGeografiskTilknytningPersonIkkeFunnet) {
        null
    }

}
