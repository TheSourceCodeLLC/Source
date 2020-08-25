plugins {
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

allprojects {
    group = "net.sourcebot"
    version = "5.0.2"
    buildDir = File(rootProject.projectDir, "target/output/$name")

    apply { plugin("org.jetbrains.kotlin.jvm") }

    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.0")
        implementation("net.dv8tion:JDA:4.2.0_194")
        implementation("ch.qos.logback:logback-classic:1.2.3")
        implementation("com.google.guava:guava:28.2-jre")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
        implementation("uk.org.lidalia:sysout-over-slf4j:1.0.2")
        implementation("org.mongodb:mongo-java-driver:3.12.4")
        implementation("org.fusesource.jansi:jansi:1.18")
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
    }
}

task("install").dependsOn(":API:shadowJar")