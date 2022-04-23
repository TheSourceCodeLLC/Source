import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

description = "Module responsible for posting games that are 100% off to a designated channel."

dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.google.code.gson:gson:2.9.0")
}

configurations.all {
    val kotlinVersion = getKotlinPluginVersion()
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
}
