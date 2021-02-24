package no.nav.helse.sparkelnorg

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal const val NAV_OPPFOLGING_UTLAND_KONTOR_NR = "0393"

private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

fun main() {
    val serviceUser = readServiceUserCredentials()
    val environment = readEnvironment()
    launchApplication(environment, serviceUser)
}

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

    val behandlendeEnhetService = PersoninfoService(norgRestClient, personV3)

    RapidApplication.create(System.getenv()).apply {
        BehandlendeEnhetRiver(this, behandlendeEnhetService)
        HentNavnRiver(this, behandlendeEnhetService)
    }.start()
}

private fun simpleHttpClient(serializer: JacksonSerializer? = JacksonSerializer()) = HttpClient {
    install(Logging) {
        level = LogLevel.BODY
        logger = object : io.ktor.client.features.logging.Logger {
            override fun log(message: String) {
                sikkerLogg.debug(message)
            }
        }
    }
    install(JsonFeature) {
        this.serializer = serializer
    }
    expectSuccess = false
}
