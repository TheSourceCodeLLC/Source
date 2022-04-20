val kotlinVersion = "1.6.0"
dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    api("org.jetbrains.kotlin:kotlin-reflect:1.6.0")
    api("net.dv8tion:JDA:4.3.0_307")
    api("ch.qos.logback:logback-classic:1.2.10")
    api("com.google.guava:guava:31.0.1-jre")
    api("com.fasterxml.jackson.core:jackson-databind:2.13.1")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    api("uk.org.lidalia:sysout-over-slf4j:1.0.2")
    api("org.mongodb:mongo-java-driver:3.12.10")
    api("org.fusesource.jansi:jansi:2.4.0")
    api("com.sedmelluq:lavaplayer:1.3.69")
    api("me.hwiggy.kommander:API:1.7.2")
    api("me.hwiggy:Extensible:1.4.5")
}

tasks.shadowJar {
    destinationDirectory.set(File(rootProject.projectDir, "target/bin"))
    manifest.attributes["Main-Class"] = "net.sourcebot.Source"
    archiveFileName.set("Source.jar")
    mergeServiceFiles()
}