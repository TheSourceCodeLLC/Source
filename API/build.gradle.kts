val kotlinVersion = "1.5.31"
dependencies {
    api(kotlin("stdlib", kotlinVersion))
    api(kotlin("reflect", kotlinVersion))
    api("net.dv8tion:JDA:4.3.0_307")
    api("ch.qos.logback:logback-classic:1.2.6")
    api("com.google.guava:guava:31.0.1-jre")
    api("com.fasterxml.jackson.core:jackson-databind:2.12.5")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.5")
    api("uk.org.lidalia:sysout-over-slf4j:1.0.2")
    api("org.mongodb:mongo-java-driver:3.12.10")
    api("org.fusesource.jansi:jansi:2.3.4")
    api("com.sedmelluq:lavaplayer:1.3.69")
    api("me.hwiggy:Kommander:1.4.6")
    api("me.hwiggy:Extensible:1.4.1")
}

tasks.shadowJar {
    destinationDirectory.set(File(rootProject.projectDir, "target/bin"))
    manifest.attributes["Main-Class"] = "net.sourcebot.Source"
    archiveFileName.set("Source.jar")
    mergeServiceFiles()
}