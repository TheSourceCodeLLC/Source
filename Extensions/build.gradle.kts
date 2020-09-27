import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

subprojects {
    dependencies { compileOnly(project(":API")) }

    val targetFolder = File(rootProject.projectDir, "target")
    val modulesFolder = File(targetFolder, "/bin/modules")
    tasks.named<ShadowJar>("shadowJar") {
        destinationDirectory.set(modulesFolder)
        archiveClassifier.set("")
        mergeServiceFiles()
    }
}