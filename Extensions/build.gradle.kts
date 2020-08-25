subprojects {
    dependencies {
        implementation(project(":API"))
    }

    val targetFolder = File(rootProject.projectDir, "target")
    tasks {
        jar { destinationDirectory.set(File(targetFolder, "/bin/modules")) }
    }
}

tasks {
    task("install") {
        dependsOn(subprojects.map { "${it.name}:jar" })
    }
}