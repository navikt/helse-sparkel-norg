package no.nav.helse.sparkelnorg

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.registerBehandlendeEnhetApi(behandlendeEnhetService: BehandlendeEnhetService){
    get("/behandlendeEnhet") {
        val fødselsnummer = requireNotNull(call.request.headers["fødselsnummer"])
        call.respond(behandlendeEnhetService.finnBehandlendeEnhet(fødselsnummer))
    }
}
