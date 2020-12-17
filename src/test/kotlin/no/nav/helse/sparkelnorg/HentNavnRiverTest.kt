package no.nav.helse.sparkelnorg

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoennstyper
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar


internal class HentNavnRiverTest {

    val rapid = TestRapid()
        .apply { HentNavnRiver(this, mockk {
            every { runBlocking { finnPerson(any()) }}.returns(
                Person()
                    .withPersonnavn(Personnavn()
                        .withFornavn("Test")
                        .withMellomnavn(null)
                        .withEtternavn("Testsen"))
                    .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                    .withFoedselsdato(Foedselsdato().withFoedselsdato(LocalDate.of(1986, 2, 23).toXml()))
            )
        }) }

    @Test
    fun `løser personoppslagbehov`() {
        rapid.sendTestMessage(behov)
        val løsning = rapid.inspektør.message(0)["@løsning"]["HentPersoninfo"]
        assertEquals("Test", løsning["fornavn"].asText())
        assertFalse(løsning.hasNonNull("mellomnavn"))
        assertEquals("Testsen", løsning["etternavn"].asText())
        assertEquals(LocalDate.of(1986, 2, 23), løsning["fødselsdato"].asText().let(LocalDate::parse))
        assertEquals(Kjønn.Mann.name, løsning["kjønn"].asText())
    }
}

@Language("JSON")
val behov = """{
  "@event_name": "behov",
  "@behov": [
    "HentPersoninfo",
    "HentEnhet"
  ],
  "@id": "2dad52b1-f58e-4c26-bb24-970705cdea67",
  "@opprettet": "2020-05-05T11:16:12.678539",
  "spleisBehovId": "c2d3ce2e-abeb-4c27-a7d3-e45f23ef26f7",
  "fødselsnummer": "12345",
  "orgnummer": "89123"
}
"""

fun LocalDate.toXml(): XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(this.toString())
