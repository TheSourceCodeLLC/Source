repositories {
    maven("https://nexus.mcdevs.us/repository/mcdevs/")
}

dependencies {
    api(kotlin("stdlib", "1.4.10"))
    api(kotlin("reflect", "1.4.10"))
    api("net.dv8tion:JDA:4.2.0_225")
    api("ch.qos.logback:logback-classic:1.2.3")
    api("com.google.guava:guava:28.2-jre")
    api("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    api("uk.org.lidalia:sysout-over-slf4j:1.0.2")
    api("org.mongodb:mongo-java-driver:3.12.4")
    api("org.fusesource.jansi:jansi:1.18")
    api("com.sedmelluq:lavaplayer:1.3.69")

    implementation("me.hwiggy:Extensible:1.2")
}

tasks.shadowJar {
    destinationDirectory.set(File(rootProject.projectDir, "target/bin"))
    manifest.attributes["Main-Class"] = "net.sourcebot.Source"
    archiveFileName.set("Source.jar")
    mergeServiceFiles()
}