package no.nav.helse.sparkelnorg

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlendeEnhetRiverTest {

    private val NAV_GØVIK = "100000090"

    @Test
    fun `happy case`() {
        every { personv3mock.hentGeografiskTilknytning(any()) }.returns(
            HentGeografiskTilknytningResponse().withGeografiskTilknytning(
                Kommune().withGeografiskTilknytning("3407")
            )
        )
        rapid.sendTestMessage(behov)
        assertEquals(NAV_GØVIK, rapid.inspektør.message(0)["@løsning"]["HentEnhet"].textValue())
    }

    @Test
    fun `404 fra norg`() {
        every { personv3mock.hentGeografiskTilknytning(any()) }.returns(
            HentGeografiskTilknytningResponse().withGeografiskTilknytning(
                Land().withGeografiskTilknytning("SWE")
            )
        )
        rapid.sendTestMessage(behov)
        assertEquals(NAV_OPPFOLGING_UTLAND_KONTOR_NR, rapid.inspektør.message(0)["@løsning"]["HentEnhet"].textValue())
    }

    @Test
    fun `500 fra norg`() {
        every { personv3mock.hentGeografiskTilknytning(any()) }.returns(
            HentGeografiskTilknytningResponse().withGeografiskTilknytning(
                Land().withGeografiskTilknytning("FOO")
            )
        )
        rapid.sendTestMessage(behov)
        assertEquals(0, rapid.inspektør.size)
    }

    @Language("JSON")
    val behov = """
        {
          "@event_name": "behov",
          "@behov": [
            "HentEnhet"
          ],
          "@id": "2dad52b1-f58e-4c26-bb24-970705cdea67",
          "@opprettet": "2020-05-05T11:16:12.678539",
          "spleisBehovId": "c2d3ce2e-abeb-4c27-a7d3-e45f23ef26f7",
          "fødselsnummer": "12345",
          "orgnummer": "89123"
        }
"""

    private val personv3mock = mockk<PersonV3> {
        every { hentPerson(any()) } returns HentPersonResponse().withPerson(
            Person()
        )
    }

    private val client = HttpClient(MockEngine) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        expectSuccess = false
        engine {
            addHandler { request ->
                when (request.url.encodedPath) {
                    "/baseurl/enhet/navkontor/SWE" -> {
                        respond(
                            "{\"field\":null,\"message\":\"Enheten med nummeret ''{0}'' eksisterer ikke\"}",
                            status = HttpStatusCode.NotFound,
                            headersOf(name = HttpHeaders.ContentType, value = "application/json")
                        )
                    }
                    "/baseurl/enhet/navkontor/3407" -> {
                        respond(
                            "{\"enhetNr\": $NAV_GØVIK }",
                            status = HttpStatusCode.OK,
                            headersOf(name = HttpHeaders.ContentType, value = "application/json")
                        )
                    }
                    else -> respond(
                        "{}",
                        status = HttpStatusCode.InternalServerError,
                        headersOf(name = HttpHeaders.ContentType, value = "application/json")
                    )
                }
            }
        }
    }

    private val norg2Client = Norg2Client("baseurl", client)
    private val rapid = TestRapid()
        .apply {
            BehandlendeEnhetRiver(this, PersoninfoService(norg2Client, personv3mock))
        }
}

