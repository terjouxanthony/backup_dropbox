plugins {
    application
    java
    id("io.freefair.lombok") version "6.0.0-m2"
    id("com.github.johnrengelman.shadow") version "6.1.0" // for fat jars
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

dependencies {
    //project dependencies:
    implementation("com.dropbox.core", "dropbox-core-sdk", "4.0.0")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.11.1")

    //logging:
    implementation("org.slf4j", "slf4j-api", "1.7.30")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")

    //testing:
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.mockito", "mockito-junit-jupiter", "2.23.0")
    testImplementation("org.assertj:assertj-core:3.17.2")
}

application {
    mainClass.set("org.example.backup_dropbox.Main")
    project.setProperty("mainClassName", mainClass.get())
    applicationDefaultJvmArgs = listOf(
        "-Xmx30m",
        "-XX:+UseSerialGC",
        "-Xss300k"
    )
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}


tasks.test {
    doFirst {
        systemProperties["junit.jupiter.execution.parallel.enabled"] = true
        systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
        useJUnitPlatform()
    }
}
