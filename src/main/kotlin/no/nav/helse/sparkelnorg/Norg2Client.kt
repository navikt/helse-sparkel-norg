package no.nav.helse.sparkelnorg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.contentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Norg2Client(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    private val log: Logger = LoggerFactory.getLogger(Norg2Client::class.java)

    suspend fun finnBehandlendeEnhet(geografiskOmraade: String, diskresjonskode: String?): Enhet =
        retry("find_local_nav_office") {
            val httpResponse = httpClient.get<HttpStatement>("$baseUrl/enhet/navkontor/$geografiskOmraade") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                if (!diskresjonskode.isNullOrEmpty()) {
                    parameter("disk", diskresjonskode)
                }
            }.execute()
            if (httpResponse.status == NotFound) {
                log.info("Fant ikke lokalt NAV-kontor for geografisk tilhørighet: $geografiskOmraade, setter da NAV-kontor oppfølging utland som lokalt navkontor: $NAV_OPPFOLGING_UTLAND_KONTOR_NR")
                Enhet(NAV_OPPFOLGING_UTLAND_KONTOR_NR)
            } else {
                httpResponse.call.response.receive()
            }
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Enhet(
    val enhetNr: String
)
