package no.nav.helse.sparkel.norg

import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import java.io.IOException
import javax.net.ssl.SSLHandshakeException
import kotlin.reflect.KClass

suspend fun <T> retry(
    callName: String,
    vararg legalExceptions: KClass<out Throwable> =
        arrayOf(
            IOException::class,
            ClosedReceiveChannelException::class,
            SSLHandshakeException::class,
        ),
    retryIntervals: Array<Long> = arrayOf(500, 1000, 3000, 5000, 10000),
    exceptionCausedByDepth: Int = 3,
    block: suspend () -> T,
): T {
    for (interval in retryIntervals) {
        try {
            return block()
        } catch (e: Throwable) {
            if (!isCausedBy(e, exceptionCausedByDepth, legalExceptions)) {
                throw e
            }
            e.loggWarn("Failed to execute callName=$callName, retrying in $interval ms", e)
        }
        delay(interval)
    }
    return block()
}

private fun isCausedBy(
    throwable: Throwable,
    depth: Int,
    legalExceptions: Array<out KClass<out Throwable>>,
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
