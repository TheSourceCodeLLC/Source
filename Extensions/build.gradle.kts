subprojects {
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
    }
}