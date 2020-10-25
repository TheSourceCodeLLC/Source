plugins {
    id("org.jetbrains.kotlin.jvm").version("1.4.10")
    id("com.github.johnrengelman.shadow").version("6.0.0")
}

allprojects {
    group = "net.sourcebot"
    version = "5.1.1"
    buildDir = File(rootProject.projectDir, "target/output/$name")

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

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
        processResources {
            filesMatching("module.json") { expand("project" to project) }
            outputs.upToDateWhen { false }
        }
    }
}

task("install") {
    dependsOn(":API:shadowJar")
    val installFile = File("INSTALL")
    if (!installFile.exists()) {
        val extensions = project(":Extensions")
        dependsOn(extensions.subprojects.map { ":Extensions:${it.name}:shadowJar" })
    } else {
        val toInstall = installFile.readLines()
        dependsOn(toInstall.map { ":Extensions:${it}:shadowJar" })
    }
}