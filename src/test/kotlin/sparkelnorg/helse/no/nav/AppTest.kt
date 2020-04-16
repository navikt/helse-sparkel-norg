package sparkelnorg.helse.no.nav

import no.nav.helse.sparkelnorg.Environment
import no.nav.helse.sparkelnorg.ServiceUser
import no.nav.helse.sparkelnorg.launchApplication
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class AppTest {

    private val testEnvironment =
        Environment("norgBasUrl", "persoUrl", "stsUrl")
    private val testServiceUser = ServiceUser("username", "password")

    @Disabled("Needs a rewrite")
    @Test
    fun `test configuration`() {
        launchApplication(testEnvironment, testServiceUser)
    }
}
