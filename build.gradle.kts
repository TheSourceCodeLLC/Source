plugins {
    id("org.jetbrains.kotlin.jvm").version("1.4.10")
    id("com.github.johnrengelman.shadow").version("6.0.0")
    id("maven-publish")
}

allprojects {
    group = "net.sourcebot"
    version = "5.2.0"
    buildDir = File(rootProject.projectDir, "target/output/$name")

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "maven-publish")

    repositories {
        jcenter()
        mavenCentral()
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

    publishing {
        repositories {
            maven {
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
        publications {
            create<MavenPublication>("maven") {
                artifact(tasks.shadowJar.get())
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
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