package no.nav.helse.sparkel.norg

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.speed.SpeedClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.helse.rapids_rivers.RapidApplication

internal const val NAV_OPPFOLGING_UTLAND_KONTOR_NR = "0393"

fun main() {
    launchApplication(System.getenv())
}

fun launchApplication(env: Map<String, String>) {
    val norgRestClient =
        Norg2Client(
            baseUrl = "http://norg2.org",
            httpClient = simpleHttpClient(),
        )
    val azureClient = createAzureTokenClientFromEnvironment(env)
    val speedClient =
        SpeedClient(
            httpClient =
                java.net.http.HttpClient
                    .newHttpClient(),
            objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()),
            tokenProvider = azureClient,
        )

    val personinfoService = PersoninfoService(norgRestClient, speedClient)

    RapidApplication
        .create(System.getenv(), builder = {
            withKtorModule {
                behandlendeEnhetApi(
                    personinfoService = personinfoService,
                    clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                    issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
                    jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
                )
            }
        })
        .apply {
            BehandlendeEnhetRiver(this, personinfoService)
        }.start()
}

private fun simpleHttpClient() =
    HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
        expectSuccess = false
    }
