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
    val norgRestClient = Norg2Client(
        baseUrl = "http://norg2.org",
        httpClient = simpleHttpClient()
    )
    val azureClient = createAzureTokenClientFromEnvironment(env)
    val speedClient = SpeedClient(
        httpClient = java.net.http.HttpClient.newHttpClient(),
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()),
        tokenProvider = azureClient
    )

    val behandlendeEnhetService = PersoninfoService(norgRestClient, speedClient)

    RapidApplication.create(System.getenv()).apply {
        BehandlendeEnhetRiver(this, behandlendeEnhetService)
    }.start()
}

private fun simpleHttpClient() = HttpClient {
    install(ContentNegotiation) {
        jackson()
    }
    expectSuccess = false
}
