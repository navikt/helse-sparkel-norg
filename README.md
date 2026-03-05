# sparkel-norg [![Build](https://github.com/navikt/helse-sparkel-norg/actions/workflows/build.yml/badge.svg)](https://github.com/navikt/helse-sparkel-norg/actions/workflows/build.yml)

Sparkelappene er mikrotjenester som svarer ut behov ved å hente data fra ulike registre.

## Hvorfor Sparkel?

Det ble tidlig klart at sykepenge-tjenesten ville måtte hente data fra en del forskjellige tjenester utenfor teamet (og etter hvert som Produktområde Helse ble grunnlagt, uten POet). Det ble også klart at det var svært mange forskjellige måter å hente denne dataen på. Noen tjenester tilbød http-apier, noen brukte GraphQL, noen brukte ActiveMQ, osv. Vi ønsket å slippe at våre kjerneapplikasjoner skulle måtte forholde seg til alle disse forskjellige protokollene, så vi laget et sett med mikrotjenester, der hver tjeneste skulle integrere mot én ekstern tjeneste hver. Dermed fikk hver mikrotjeneste én protokoll å forholde seg til, og kjerneapplikasjonene kunne bruke samme måte å hente data på uavhengig av kilde.

Vi tenkte at en slik harmonisering av grensesnitt fungerte som et lag sparkel over en ujevn vegg, så da var fellesnavnet for disse applikasjonene selvsagt: Sparkel.

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X)
