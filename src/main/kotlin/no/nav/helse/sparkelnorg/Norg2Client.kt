package no.nav.helse.sparkelnorg

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import kotlinx.io.errors.IOException
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

@KtorExperimentalAPI
class Norg2Client(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {

    private val log: Logger = LoggerFactory.getLogger(Norg2Client::class.java)
    suspend fun getLocalNAVOffice(geografiskOmraade: String, diskresjonskode: String?): Enhet =
        retry("find_local_nav_office") {
            val httpResponse = httpClient.get<HttpResponse>("$baseUrl/enhet/navkontor/$geografiskOmraade") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                if (!diskresjonskode.isNullOrEmpty()) {
                    parameter("disk", diskresjonskode)
                }
            }
            if (httpResponse.status == NotFound) {
                log.info("Fant ikke lokalt NAV-kontor for geografisk tilhørighet: $geografiskOmraade, setter da NAV-kontor oppfølging utland som lokalt navkontor: $NAV_OPPFOLGING_UTLAND_KONTOR_NR")
                Enhet(NAV_OPPFOLGING_UTLAND_KONTOR_NR)
            } else {
                httpResponse.call.response.receive()
            }
        }

    private suspend fun <T> retry(
        callName: String,
        vararg legalExceptions: KClass<out Throwable> = arrayOf(IOException::class),
        retryIntervals: Array<Long> = arrayOf(500, 1000, 3000, 5000, 10000),
        exceptionCausedByDepth: Int = 3,
        block: suspend () -> T
    ): T {
        for (interval in retryIntervals) {
            try {
                return block()
            } catch (e: Throwable) {
                if (!isCausedBy(e, exceptionCausedByDepth, legalExceptions)) {
                    throw e
                }
                log.warn("Failed to execute {}, retrying in $interval ms", keyValue("callName", callName), e)
            }
            delay(interval)
        }
        return block()
    }

    private fun isCausedBy(
        throwable: Throwable,
        depth: Int,
        legalExceptions: Array<out KClass<out Throwable>>
    ): Boolean {
        var current: Throwable = throwable
        for (i in 0.until(depth)) {
            if (legalExceptions.any { it.isInstance(current) }) {
                return true
            }
            current = current.cause ?: break
        }
        return false
    }
}


data class Enhet(
    val enhetNr: String
)