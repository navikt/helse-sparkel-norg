package no.nav.helse.sparkelnorg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import java.util.concurrent.TimeUnit

val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
internal val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())

internal const val NAV_OPPFOLGING_UTLAND_KONTOR_NR = "0393"

@KtorExperimentalAPI
fun main() {
    val serviceUser = readServiceUserCredentials()
    val environment = readEnvironment()
    RapidApplication.create(System.getenv()).apply {

    }
    launchApplication(environment, serviceUser)
}

@KtorExperimentalAPI
fun launchApplication(
    environment: Environment,
    serviceUser: ServiceUser
) {
    val norgRestClient = Norg2Client(
        baseUrl = environment.norg2BaseUrl,
        httpClient = simpleHttpClient()
    )

    val personV3 = createPort<PersonV3>(environment.personV3Url) {
        port {
            withSTS(
                serviceUser.username,
                serviceUser.password,
                environment.securityTokenServiceUrl
            )
        }
    }

    val behandlendeEnhetService = BehandlendeEnhetService(norgRestClient, personV3)

    RapidApplication.create(System.getenv()).apply {
        BehandlendeEnhetRiver(this, behandlendeEnhetService)
    }
}

private fun simpleHttpClient(serializer: JacksonSerializer? = JacksonSerializer()) = HttpClient() {
    install(JsonFeature) {
        this.serializer = serializer
    }
}
