plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
}

subprojects {
  apply(plugin = "org.jetbrains.dokka")
}

repositories {
  mavenCentral()
}

