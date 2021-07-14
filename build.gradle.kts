plugins {
    id("org.jetbrains.kotlin.jvm").version("1.4.10")
    id("com.github.johnrengelman.shadow").version("6.1.0")
    id("maven-publish")
    id("java-library")
}

allprojects {
    group = "net.sourcebot"
    version = "5.2.7"
    buildDir = File(rootProject.projectDir, "target/output/$name")

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")

    repositories {
        jcenter()
        mavenLocal()
        mavenCentral()
        maven("https://nexus.mcdevs.us/repository/mcdevs/")
    }

    tasks {
        compileJava {
            sourceCompatibility = "11"
            targetCompatibility = "11"
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

    java {
        withSourcesJar()
        withJavadocJar()
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
}

task("install") {
    val depends = File("INSTALL").let { file ->
        val subprojects = project(":Extensions").subprojects.map { it.name }
        (if (!file.exists()) subprojects else file.readLines()).map {
            ":Extensions:${it}:shadowJar"
        }
    }
    dependsOn(":API:shadowJar", *depends.toTypedArray())
}