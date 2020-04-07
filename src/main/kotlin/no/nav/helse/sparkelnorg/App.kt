package no.nav.helse.sparkelnorg

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3

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

    val behandlendeEnhetService = PersoninfoService(norgRestClient, personV3)

    RapidApplication.create(System.getenv()).apply {
        BehandlendeEnhetRiver(this, behandlendeEnhetService)
        HentNavnRiver(this, behandlendeEnhetService)
    }
}

private fun simpleHttpClient(serializer: JacksonSerializer? = JacksonSerializer()) = HttpClient {
    install(JsonFeature) {
        this.serializer = serializer
    }
}
