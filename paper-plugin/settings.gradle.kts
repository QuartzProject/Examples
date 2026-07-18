rootProject.name = "quartz-paper-example"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // Embed is not on the Gradle Plugin Portal yet: it is built from a checkout, the sibling clone by default.
    includeBuild(providers.gradleProperty("quartz.embed").getOrElse("../../Embed"))
}

includeBuild(providers.gradleProperty("quartz.embed").getOrElse("../../Embed"))

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include("bridge", "plugin")
