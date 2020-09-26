import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

subprojects {
    dependencies { compileOnly(project(":API", "shadow")) }
    apply { plugin("com.github.johnrengelman.shadow") }

    val targetFolder = File(rootProject.projectDir, "target")
    val modulesFolder = File(targetFolder, "/bin/modules")
    tasks {
        register<Delete>("deleteOld") {
            delete(fileTree(modulesFolder).include("${project.name}-*.jar"))
        }
        named<ShadowJar>("shadowJar") {
            dependsOn("deleteOld")
            destinationDirectory.set(modulesFolder)
            archiveClassifier.set("")
        }
    }
}

task("install").dependsOn(subprojects.map { "${it.name}:shadowJar" })