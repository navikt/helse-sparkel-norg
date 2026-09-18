package no.nav.helse.sparkel.norg

import com.github.navikt.tbd_libs.result_object.Result

class SpeedException(
    message: String,
    cause: Throwable?,
) : RuntimeException(message, cause)

// Egen exception-type slik at retry kan skille feilende kall mot Speed fra programmeringsfeil i egen kode
fun <T> Result<T>.getOrThrowSpeedException(): T =
    when (this) {
        is Result.Ok -> value
        is Result.Error -> throw SpeedException(error, cause)
    }
