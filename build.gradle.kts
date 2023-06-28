plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("org.jetbrains.dokka") version "1.8.20"
  id("org.jmailen.kotlinter") version "3.15.0" apply false
}

subprojects {
  apply(plugin = "org.jetbrains.dokka")
}

repositories {
  mavenCentral()
}

