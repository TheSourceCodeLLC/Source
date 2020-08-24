group = "net.sourcebot"
version = "5.0.2"


subprojects {
    dependencies {
        implementation(project(":API"))
    }

    tasks {
        jar {
            destinationDirectory.set(file("${project.rootDir}/release/modules"))
        }

    }
}