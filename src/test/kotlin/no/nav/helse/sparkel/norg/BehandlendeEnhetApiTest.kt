package no.nav.helse.sparkel.norg

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.createDefaultAzureTokenClient
import com.github.navikt.tbd_libs.speed.SpeedClient
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.jackson.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import java.net.ServerSocket
import java.net.URI
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val CLIENT_ID = "sparkel-norg-junit"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlendeEnhetApiTest {
    private val mockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start)
    private val norgWireMock = WireMockServer(wireMockConfig().dynamicPort()).also(WireMockServer::start)
    private val speedWireMock = WireMockServer(wireMockConfig().dynamicPort()).also(WireMockServer::start)
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val port = ServerSocket(0).use { it.localPort }
    private val serverUrl = "http://localhost:$port"

    private val embeddedServer = embeddedServer(CIO, port = port) {
        behandlendeEnhetApi(
            personinfoService = PersoninfoService(
                norg2Client = Norg2Client(
                    baseUrl = norgWireMock.baseUrl(),
                    httpClient = HttpClient(ClientCIO) {
                        install(ClientContentNegotiation) { jackson() }
                        expectSuccess = false
                    }
                ),
                speedClient = SpeedClient(
                    httpClient = java.net.http.HttpClient.newHttpClient(),
                    objectMapper = objectMapper,
                    tokenProvider = createDefaultAzureTokenClient(
                        tokenEndpoint = URI(mockOAuth2Server.tokenEndpointUrl("default").toString()),
                        clientId = "mockClientId",
                        clientSecret = "mockClientSecret",
                    ),
                    baseUrl = speedWireMock.baseUrl(),
                    scope = "test-scope"
                )
            ),
            clientId = CLIENT_ID,
            issuerUrl = mockOAuth2Server.issuerUrl("default").toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl("default").toString(),
        )
    }.start(wait = false)

    @AfterAll
    fun teardown() {
        embeddedServer.stop()
        norgWireMock.stop()
        speedWireMock.stop()
        mockOAuth2Server.shutdown()
    }

    @BeforeEach
    fun resetMocks() {
        norgWireMock.resetAll()
        speedWireMock.resetAll()
    }

    @Test
    fun `gir forventet svar for en person med bydel`() {
        // Given:
        val kommune = "0301"
        val bydel = "${kommune}01"
        stubPerson()
        stubGeografiskTilknytning(
            """
                {
                    "type": "BYDEL",
                    "land": null,
                    "kommune": "$kommune",
                    "bydel": "$bydel",
                    "kilde": "PDL"
                }
            """.trimIndent()
        )
        stubNorg(
            geografiskOmraade = bydel,
            json = """
                {
                    "enhetNr": "1337",
                    "navn": "Nav Gamle Oslo",
                    "type": "LOKAL"
                }
            """.trimIndent()
        )

        // When:
        val (statusCode, body) = postBehandlendeEnhet(bearerToken())

        // Then:
        assertEquals(200, statusCode)
        assertJsonEquals(
            """
            {
                "enhetNr": "1337",
                "navn": "Nav Gamle Oslo",
                "type": "LOKAL"
            }
            """.trimIndent(),
            body,
        )
    }

    @Test
    fun `gir forventet svar for en person med kommune`() {
        // Given:
        val kommune = "0301"
        stubPerson()
        stubGeografiskTilknytning(
            """
                {
                    "type": "KOMMUNE",
                    "land": null,
                    "kommune": "$kommune",
                    "bydel": null,
                    "kilde": "PDL"
                }
            """.trimIndent()
        )
        stubNorg(
            geografiskOmraade = kommune,
            json = """
                {
                    "enhetNr": "1234",
                    "navn": "Nav Oslo",
                    "type": "LOKAL"
                }
            """.trimIndent()
        )

        // When:
        val (statusCode, body) = postBehandlendeEnhet(bearerToken())

        // Then:
        assertEquals(200, statusCode)
        assertJsonEquals(
            """
            {
                "enhetNr": "1234",
                "navn": "Nav Oslo",
                "type": "LOKAL"
            }
            """.trimIndent(),
            body,
        )
    }

    @Test
    fun `gir forventet svar for en person med utland`() {
        // Given:
        val land = "AUS"
        stubPerson()
        stubGeografiskTilknytning(
            """
                {
                    "type": "UTLAND",
                    "land": "AUS",
                    "kommune": null,
                    "bydel": null,
                    "kilde": "PDL"
                }
            """.trimIndent()
        )
        stubNorg(
            geografiskOmraade = land,
            json = """
                {
                    "enhetNr": "1234",
                    "navn": "Nav Oslo",
                    "type": "LOKAL"
                }
            """.trimIndent()
        )

        // When:
        val (statusCode, body) = postBehandlendeEnhet(bearerToken())

        // Then:
        assertEquals(200, statusCode)
        assertJsonEquals(
            """
            {
                "enhetNr": "1234",
                "navn": "Nav Oslo",
                "type": "LOKAL"
            }
            """.trimIndent(),
            body,
        )
    }

    @Test
    fun `gir forventet svar for en person med adressebeskyttelse fortrolig`() {
        // Given:
        stubPerson(adressebeskyttelse = "STRENGT_FORTROLIG")
        stubGeografiskTilknytning(
            json = """
                {
                    "type": "KOMMUNE",
                    "land": null,
                    "kommune": "3407",
                    "bydel": null,
                    "kilde": "PDL"
                }
            """.trimIndent()
        )
        norgWireMock.stubFor(
            get(urlPathEqualTo("/norg2/api/v1/enhet/navkontor/3407"))
                .withQueryParam("disk", equalTo("SPSF"))
                .willReturn(
                    okJson(
                        """
                        {
                            "enhetNr": "100000090",
                            "navn": "Nav Gøvik",
                            "type": "LOKAL"
                        }
                        """.trimIndent()
                    )
                )
        )

        // When:
        val (statusCode, body) = postBehandlendeEnhet(bearerToken())

        // Then:
        assertEquals(200, statusCode)
        assertJsonEquals(
            """
            {
                "enhetNr": "100000090",
                "navn": "Nav Gøvik",
                "type": "LOKAL"
            }
            """.trimIndent(),
            body,
        )
    }

    @Test
    fun `returnerer 404 når norg ikke kjenner til geografisk område`() {
        // Given:
        val land = "SWE"
        stubPerson()
        stubGeografiskTilknytning(
            json = """
                {
                    "type": "UTLAND",
                    "land": "$land",
                    "kommune": null,
                    "bydel": null,
                    "kilde": "PDL"
                }
            """.trimIndent()
        )
        norgWireMock.stubFor(
            get(urlPathEqualTo("/norg2/api/v1/enhet/navkontor/$land")).willReturn(
                aResponse().withStatus(404).withHeader("Content-Type", "application/json")
                    .withBody("""{ "message": "Enheten eksisterer ikke" }""")
            )
        )

        // When:
        val (statusCode, _) = postBehandlendeEnhet(bearerToken())

        // Then:
        assertEquals(404, statusCode)
    }

    @Test
    fun `returnerer 401 uten autentiseringstoken`() {
        val (statusCode, _) = postBehandlendeEnhet(token = null)

        assertEquals(401, statusCode)
    }

    @Test
    fun `returnerer 401 med token med feil audience`() {
        val (statusCode, _) = postBehandlendeEnhet(bearerToken(audience = "feil-audience"))

        assertEquals(401, statusCode)
    }

    @Test
    fun `returnerer 401 med token fra feil issuer`() {
        val (statusCode, _) = postBehandlendeEnhet(bearerToken(issuerId = "feil-issuer"))

        assertEquals(401, statusCode)
    }

    private fun bearerToken(issuerId: String = "default", audience: String = CLIENT_ID): String =
        mockOAuth2Server.issueToken(issuerId = issuerId, audience = audience).serialize()

    private fun postBehandlendeEnhet(token: String?): Pair<Int, String> =
        Request.post("${serverUrl}/api/behandlende-enhet")
            .bodyString("""{ "identitetsnummer": "12345678901" }""", ContentType.APPLICATION_JSON)
            .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
            .execute()
            .handleResponse { response -> response.code to (EntityUtils.toString(response.entity) ?: "") }

    private fun stubPerson(adressebeskyttelse: String = "UGRADERT") {
        speedWireMock.stubFor(
            post(urlPathEqualTo("/api/person")).willReturn(
                okJson(
                    """
                            {
                                "fødselsdato": "1990-01-01",
                                "dødsdato": null,
                                "fornavn": "Test",
                                "mellomnavn": null,
                                "etternavn": "Testesen",
                                "adressebeskyttelse": "$adressebeskyttelse",
                                "kjønn": "MANN"
                            }
                            """.trimIndent()
                )
            )
        )
    }

    private fun stubGeografiskTilknytning(@Language("json") json: String) {
        speedWireMock.stubFor(
            post(urlPathEqualTo("/api/geografisk_tilknytning")).willReturn(
                okJson(json)
            )
        )
    }

    private fun stubNorg(geografiskOmraade: String, @Language("json") json: String) {
        norgWireMock.stubFor(
            get(urlPathEqualTo("/norg2/api/v1/enhet/navkontor/$geografiskOmraade")).willReturn(
                okJson(json)
            )
        )
    }

    private fun assertJsonEquals(@Language("json") expected: String, actual: String) {
        assertEquals(objectMapper.readTree(expected), objectMapper.readTree(actual))
    }
}

