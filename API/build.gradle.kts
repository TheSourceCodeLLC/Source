import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("com.github.johnrengelman.shadow")
}

application { mainClassName = "net.sourcebot.Source" }

dependencies {
    api(kotlin("stdlib", "1.4.0"))
    api(kotlin("reflect", "1.4.0"))
    api("net.dv8tion:JDA:4.2.0_204")
    api("ch.qos.logback:logback-classic:1.2.3")
    api("com.google.guava:guava:28.2-jre")
    api("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    api("uk.org.lidalia:sysout-over-slf4j:1.0.2")
    api("org.mongodb:mongo-java-driver:3.12.4")
    api("org.fusesource.jansi:jansi:1.18")
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        manifest { attributes(mapOf("Main-Class" to "net.sourcebot.Source")) }
        destinationDirectory.set(File(rootProject.projectDir, "target/bin"))
        archiveFileName.set("Source.jar")
    }
}