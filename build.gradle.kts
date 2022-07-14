plugins {
    id("org.jetbrains.kotlin.jvm").version("1.6.0") apply false
    id("com.github.johnrengelman.shadow").version("6.1.0") apply false
}

allprojects {
    group = "net.sourcebot"
    version = "5.5.1"
    buildDir = File(rootProject.projectDir, "target/output/$name")

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://nexus.mcdevs.us/repository/mcdevs/")
        maven("https://m2.dv8tion.net/releases")
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