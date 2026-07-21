plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("8.6.1").apply(false)
    id("com.android.library").version("8.6.1").apply(false)
    kotlin("android").version("2.1.21").apply(false)
    kotlin("multiplatform").version("2.1.21").apply(false)
    id("com.google.devtools.ksp").version("2.1.21-2.0.1").apply(false)
    id("org.jetbrains.compose").version("1.7.3").apply(false)
    kotlin("plugin.compose").version("2.1.21").apply(false)
}

buildscript {
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
    dependencies {
        classpath(BuildPlugin.kuikly)
    }
}
