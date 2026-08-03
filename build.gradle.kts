group = "no.nav.helse.sparkel"

plugins {
    alias(libs.plugins.sas.deployable)
}

sasDeployable {
    mainClass = "no.nav.helse.sparkel.norg.AppKt"
}

dependencies {
    implementation(libs.rapidsAndRivers)
    implementation(libs.tbdLibs.azureTokenClientDefault)
    implementation(libs.tbdLibs.speedClient)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logback)

    testImplementation(libs.mockk)
    testImplementation(libs.wiremock)
    testImplementation(libs.httpclient5.fluent)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testImplementation(libs.ktor.client.mock)
}
