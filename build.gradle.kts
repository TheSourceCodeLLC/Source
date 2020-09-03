plugins {
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

allprojects {
    group = "net.sourcebot"
    version = "5.0.4"
    buildDir = File(rootProject.projectDir, "target/output/$name")

    apply { plugin("org.jetbrains.kotlin.jvm") }

    repositories {
        jcenter()
        mavenCentral()
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "11"
            kotlinOptions.freeCompilerArgs = List(1) {
                "-Xjvm-default=enable"
            }
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "11"
        }

        processResources {
            filesMatching("module.json") { expand("project" to project) }
        }
    }
}

task("install").dependsOn(":API:shadowJar")