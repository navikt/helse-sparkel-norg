package no.nav.helse.sparkel.norg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.helse.sparkel.retry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Norg2Client(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    private val log: Logger = LoggerFactory.getLogger(Norg2Client::class.java)

    suspend fun finnBehandlendeEnhet(geografiskOmraade: String, adresseBeskyttelse: String?): Enhet =
        retry("find_local_nav_office") {
            val httpResponse = httpClient.prepareGet("$baseUrl/norg2/api/v1/enhet/navkontor/$geografiskOmraade") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                if (!adresseBeskyttelse.isNullOrEmpty()) {
                    parameter("disk", adresseBeskyttelse)
                }
            }.execute()
            when {
                httpResponse.status.isSuccess() -> {
                    httpResponse.call.response.body()
                }
                httpResponse.status == NotFound -> {
                    log.info("Fant ikke lokalt NAV-kontor for geografisk tilhørighet: $geografiskOmraade, setter da NAV-kontor oppfølging utland som lokalt navkontor: $NAV_OPPFOLGING_UTLAND_KONTOR_NR")
                    Enhet(NAV_OPPFOLGING_UTLAND_KONTOR_NR)
                }
                else -> {
                    throw ClientRequestException(
                        httpResponse,
                        "Statuskode: ${httpResponse.status.description} feil på oppslag mot behandlende enhet på geografisk område: $geografiskOmraade"
                    )
                }
            }
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Enhet(
    val enhetNr: String
)
