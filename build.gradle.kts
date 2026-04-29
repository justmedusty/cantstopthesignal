
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
}

group = "com.freedom"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(26)
}
dependencies {
    implementation(ktorLibs.serialization.jackson)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.thymeleaf)
    implementation(libs.flaxoos.ktorServerRateLimiting)
    implementation(libs.logback.classic)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    runtimeOnly("org.pgpainless:pgpainless-core:2.0.3")
    implementation("org.bouncycastle:bcprov-jdk18on:1.84")
    implementation("org.bouncycastle:bcpg-jdk18on:1.84")
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
