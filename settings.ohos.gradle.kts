pluginManagement {
    resolutionStrategy {
        eachPlugin {
            val kotlinVersion = requested.version ?: "2.0.21-KBA-010"
            if (requested.id.id == "org.jetbrains.kotlin.plugin.compose") {
                useModule("org.jetbrains.kotlin:compose-compiler-gradle-plugin:$kotlinVersion")
            } else if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
            }
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        maven {
            name = "Kuikly"
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            name = "Kuikly"
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
    }
}

rootProject.name = "CityCapsule"
rootProject.buildFileName = "build.ohos.gradle.kts"

include(":shared")
project(":shared").buildFileName = "build.ohos.gradle.kts"
