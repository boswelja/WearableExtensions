buildscript {
    val kotlinVersion = "1.4.31"

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0-alpha13")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.google.gms:google-services:4.3.5")
        classpath("com.squareup.wire:wire-gradle-plugin:3.7.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
