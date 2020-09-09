import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

subprojects {
    dependencies { compileOnly(project(":API")) }
    apply { plugin("com.github.johnrengelman.shadow") }

    val targetFolder = File(rootProject.projectDir, "target")
    tasks {
        register<Delete>("deleteOld") {
            delete(
                fileTree(File(targetFolder, "/bin/modules")).include(
                    "${project.name}*.jar"
                )
            )
        }
        named<ShadowJar>("shadowJar") {
            dependsOn("deleteOld")
            destinationDirectory.set(File(targetFolder, "/bin/modules"))
        }
    }
}

tasks { task("install") { dependsOn(subprojects.map { "${it.name}:shadowJar" }) } }