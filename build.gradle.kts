import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.4.0"
    application
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

group = "net.sourcebot"
version = "5.0.2"

allprojects {

    apply {
        plugin("org.jetbrains.kotlin.jvm")
    }

    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies{
        implementation(kotlin("stdlib-jdk8"))
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
            kotlinOptions.freeCompilerArgs = List<String>(1){
                "-Xjvm-default=enable"
            }
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "11"
        }

    }


}

dependencies {
    //Self containing jar
    implementation(project(":API"))
}

application {
    mainClassName = "net.sourcebot.Source"
}

tasks {
    named<ShadowJar>("shadowJar"){
        archiveBaseName.set("Sourcebot")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "net.sourcebot"))
        }
        destinationDirectory.set(file("${this.project.rootDir}/release"))
    }
}
