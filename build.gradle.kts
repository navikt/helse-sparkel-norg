group = "no.nav.helse.sparkel"

plugins {
    id("application")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

application {
    mainClass.set("no.nav.helse.sparkel.norg.AppKt")
    applicationName = "app"
}

ktlint {
    filter {
        exclude { it.file.path.contains("test") }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    named<Test>("test") {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

dependencies {
    implementation(libs.rapidsAndRivers)
    implementation(libs.tbdLibs.azureTokenClientDefault)
    implementation(libs.tbdLibs.speedClient)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.logback)

    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation(libs.mockk)
    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testImplementation(libs.ktor.client.mock)
}
