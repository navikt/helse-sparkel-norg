package no.nav.helse.sparkel.norg

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.result_object.ok
import com.github.navikt.tbd_libs.speed.GeografiskTilknytningResponse
import com.github.navikt.tbd_libs.speed.IdentResponse
import com.github.navikt.tbd_libs.speed.PersonResponse
import com.github.navikt.tbd_libs.speed.SpeedClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BehandlendeEnhetRiverTest {

    private val NAV_GØVIK = "100000090"

    @BeforeEach
    fun resetMock() {
        clearAllMocks()
        rapid.reset()

        every { speedClient.hentPersoninfo(any(), any()) } returns PersonResponse(
            fødselsdato = LocalDate.now(),
            dødsdato = null,
            fornavn = "",
            mellomnavn = null,
            etternavn = "",
            adressebeskyttelse = PersonResponse.Adressebeskyttelse.FORTROLIG,
            kjønn = PersonResponse.Kjønn.MANN
        ).ok()
    }

    @Test
    fun `happy case`() {
        every { speedClient.hentGeografiskTilknytning(any(), any()) } returns GeografiskTilknytningResponse(
            type = GeografiskTilknytningResponse.GeografiskTilknytningType.BYDEL,
            land = null,
            kommune = "3407",
            bydel = null,
            kilde = IdentResponse.KildeResponse.PDL
        ).ok()
        rapid.sendTestMessage(behov)
        assertEquals(NAV_GØVIK, rapid.inspektør.message(0)["@løsning"]["HentEnhet"].textValue())
    }

    @Test
    fun `404 fra norg`() {
        every { speedClient.hentGeografiskTilknytning(any(), any()) } returns GeografiskTilknytningResponse(
            type = GeografiskTilknytningResponse.GeografiskTilknytningType.UTLAND,
            land = "SWE",
            kommune = null,
            bydel = null,
            kilde = IdentResponse.KildeResponse.PDL
        ).ok()
        rapid.sendTestMessage(behov)
        assertEquals(NAV_OPPFOLGING_UTLAND_KONTOR_NR, rapid.inspektør.message(0)["@løsning"]["HentEnhet"].textValue())
    }

    @Test
    fun `500 fra norg`() {
        every { speedClient.hentGeografiskTilknytning(any(), any()) } returns GeografiskTilknytningResponse(
            type = GeografiskTilknytningResponse.GeografiskTilknytningType.UTLAND_UKJENT,
            land = null,
            kommune = null,
            bydel = null,
            kilde = IdentResponse.KildeResponse.PDL
        ).ok()
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
          "hendelseId": "c2d3ce2e-abeb-4c27-a7d3-e45f23ef26f7",
          "fødselsnummer": "12345",
          "orgnummer": "89123"
        }
"""

    private val speedClient = mockk<SpeedClient>()

    private val client = HttpClient(MockEngine) {
        install(ContentNegotiation) {
            jackson()
        }
        expectSuccess = false
        engine {
            addHandler { request ->
                when (request.url.encodedPath) {
                    "/baseurl/norg2/api/v1/enhet/navkontor/SWE" -> {
                        respond(
                            "{\"field\":null,\"message\":\"Enheten med nummeret ''{0}'' eksisterer ikke\"}",
                            status = HttpStatusCode.NotFound,
                            headersOf(name = HttpHeaders.ContentType, value = "application/json")
                        )
                    }
                    "/baseurl/norg2/api/v1/enhet/navkontor/3407" -> {
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
            BehandlendeEnhetRiver(this, PersoninfoService(norg2Client, speedClient))
        }
}

