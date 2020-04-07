package no.nav.helse.sparkelnorg

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
fun Routing.registerBehandlendeEnhetApi(personinfoService: PersoninfoService){
    get("/behandlendeEnhet") {
        val fødselsnummer = requireNotNull(call.request.headers["fødselsnummer"])
        call.respond(personinfoService.finnBehandlendeEnhet(fødselsnummer))
    }
}
