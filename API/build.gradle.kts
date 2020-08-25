import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

application {
    mainClassName = "net.sourcebot.Source"
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        manifest { attributes(mapOf("Main-Class" to "net.sourcebot.Source")) }
        destinationDirectory.set(File(rootProject.projectDir, "target/bin"))
        archiveFileName.set("Source.jar")
    }
}
