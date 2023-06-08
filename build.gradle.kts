plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("org.jetbrains.dokka") version "1.8.20"
}

subprojects {
  apply(plugin = "org.jetbrains.dokka")
}

repositories {
  mavenCentral()
}

