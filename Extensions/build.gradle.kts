plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.github.johnrengelman.shadow")
    id("maven-publish")
    id("java-library")
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")


    dependencies { compileOnly(project(":API")) }

    val targetFolder = File(rootProject.projectDir, "target")
    val modulesFolder = File(targetFolder, "/bin/modules")
    tasks {
        register<Delete>("deleteOld") {
            delete(fileTree(modulesFolder).include("${project.name}-*.jar"))
        }
        shadowJar {
            dependsOn("deleteOld")
            destinationDirectory.set(modulesFolder)
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