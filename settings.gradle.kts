pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "Kuikly"
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
            content {
                includeGroup("com.tencent.kuikly-open")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "Kuikly"
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
            content {
                includeGroup("com.tencent.kuikly-open")
            }
        }
    }
}

rootProject.name = "CityCapsule"
include(":androidApp")
include(":shared")
include(":h5App")
include(":miniApp")
