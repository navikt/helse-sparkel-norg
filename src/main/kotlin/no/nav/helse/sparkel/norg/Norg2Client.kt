package no.nav.helse.sparkel.norg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.NotFound

class Norg2Client(
    private val baseUrl: String,
    private val httpClient: HttpClient,
) {
    suspend fun finnBehandlendeEnhet(
        geografiskOmraade: String,
        adresseBeskyttelse: String?,
    ): Enhet? =
        retry("find_local_nav_office") {
            val httpResponse =
                httpClient
                    .prepareGet("$baseUrl/norg2/api/v1/enhet/navkontor/$geografiskOmraade") {
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
                    null
                }

                else -> {
                    throw ClientRequestException(
                        httpResponse,
                        "Statuskode: ${httpResponse.status.description} feil på oppslag mot behandlende enhet på geografisk område: $geografiskOmraade",
                    )
                }
            }
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Enhet(
    val enhetNr: String,
    val navn: String,
    val type: String,
)
