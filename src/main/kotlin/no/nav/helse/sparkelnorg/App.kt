package no.nav.helse.sparkelnorg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.util.KtorExperimentalAPI

internal val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())

internal const val NAV_OPPFOLGING_UTLAND_KONTOR_NR = "0393"

@KtorExperimentalAPI
fun main() {
    val serviceUser = readServiceUserCredentials()
}

@KtorExperimentalAPI
fun launchApplication(
    environment: Map<String, String>
) {
    val norgRestClient = Norg2Client(
        baseUrl = environment.getValue("NORG2_BASE_URL"),
        httpClient = simpleHttpClient()
    )
}

private fun simpleHttpClient(serializer: JacksonSerializer? = JacksonSerializer()) = HttpClient() {
    install(JsonFeature) {
        this.serializer = serializer
    }
}
