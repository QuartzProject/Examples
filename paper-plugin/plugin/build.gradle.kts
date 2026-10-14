import org.quartzlang.embed.gradle.QuartzSeverity

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("org.quartzlang.embed")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    implementation(project(":bridge"))
}

quartz {
    // The @QuartzFunction classes live in :bridge, so the facades are sources this project's Kotlin can call.
    scanOwnClasses = false
    hostPackage = "org.quartzlang.examples.paper"
    facadePackage = "org.quartzlang.examples.paper.quartz"
    moduleName = "example"
    warnings = QuartzSeverity.ERROR
    // Paper's own classes are the handles; the bridge's extension functions are their methods in Quartz.
    handle("Player", "org.bukkit.entity.Player")
    handle("Sender", "org.bukkit.command.CommandSender")
    // Their KDoc becomes the documentation of the generated interface.
    hostSources.from(project(":bridge").file("src/main/kotlin"))
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}

tasks.shadowJar {
    archiveBaseName = "QuartzExample"
    archiveClassifier = ""
    filesMatching("META-INF/*.kotlin_module") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
