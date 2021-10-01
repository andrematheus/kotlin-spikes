plugins {
    kotlin("jvm") version "1.5.31"
}

group = "br.net.andrematheus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("io.kotest:kotest-runner-junit5:5.0.0.M2")
    testImplementation("io.kotest:kotest-assertions-core:5.0.0.M2")
    testImplementation("io.kotest:kotest-property:5.0.0.M2")
}