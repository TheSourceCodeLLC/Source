val kotlinVersion = "1.6.0"

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.github.johnrengelman.shadow")
    id("maven-publish")
    id("java-library")
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    api("org.jetbrains.kotlin:kotlin-reflect:1.6.0")
    api("net.dv8tion:JDA:4.3.0_307")
    api("ch.qos.logback:logback-classic:1.2.11")
    api("com.google.guava:guava:31.1-jre")
    api("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    api("uk.org.lidalia:sysout-over-slf4j:1.0.2")
    api("org.mongodb:mongo-java-driver:3.12.11")
    api("org.fusesource.jansi:jansi:2.4.0")
    api("com.sedmelluq:lavaplayer:1.3.77")
    api("me.hwiggy.kommander:API:1.7.2")
    api("me.hwiggy:Extensible:1.4.5")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    shadowJar {
        destinationDirectory.set(File(rootProject.projectDir, "target/bin"))
        manifest.attributes["Main-Class"] = "net.sourcebot.Source"
        archiveFileName.set("Source.jar")
        mergeServiceFiles()
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = List(1) {
            "-Xjvm-default=enable"
        }
    }
    processResources {
        filesMatching("module.json") { expand("project" to project) }
        outputs.upToDateWhen { false }
    }
}

publishing {
    repositories {
        when (project.findProperty("deploy") ?: "local") {
            "local" -> mavenLocal()
            "remote" -> maven {
                if (project.version.toString().endsWith("-SNAPSHOT")) {
                    setUrl("https://nexus.dveloped.net/repository/dveloped-snapshots/")
                    mavenContent { snapshotsOnly() }
                } else {
                    setUrl("https://nexus.dveloped.net/repository/dveloped-releases/")
                    mavenContent { releasesOnly() }
                }
                credentials {
                    username = System.getenv("NEXUS_USERNAME")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }
        }
    }
    publications {
        create<MavenPublication>("assembly") {
            from(components["java"])
        }
    }
}