/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin library project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/8.1.1/userguide/building_java_projects.html
 * This project uses @Incubating APIs which are subject to change.
 */

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.8.20")
    classpath("org.gradle.toolchains:foojay-resolver:0.5.0")
  }
}

//
// Plugins
//
plugins {
  java
  idea
  `java-library`
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("com.github.ben-manes.versions") version "0.47.0"
  id("org.barfuin.gradle.taskinfo") version "2.1.0"
}

// Repositories for Library Dependencies
repositories {
  mavenCentral()
}

// Library Dependencies
dependencies {

  val moshiVersion = "1.15.0"

  // These dependencies are exported to consumers, that is to say found on their compile classpath.
  api(group = "com.michael-bull.kotlin-result", name = "kotlin-result", version = "1.1.18")

  // These dependencies are used internally, and not exposed to consumers on their own compile classpath.

  // Other
  implementation(group = "com.squareup.okhttp3", name = "okhttp", version = "4.11.0")
  implementation(group = "io.github.nefilim.kjwt", name="kJWT", version="0.1.6")
  implementation(group = "com.auth0", name = "java-jwt", version = "4.4.0")
  implementation(group = "com.squareup.moshi", name = "moshi", version = moshiVersion)
  implementation(group = "com.squareup.moshi", name = "moshi-adapters", version = moshiVersion)
  implementation(group = "com.squareup.moshi", name = "moshi-kotlin", version = moshiVersion)

  // Logging
  implementation(group = "io.github.microutils", name = "kotlin-logging-jvm", version = "3.0.5")
  implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.4.8")

  // Testing
  testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version="5.7.1")
  testImplementation(group = "com.willowtreeapps.assertk", name = "assertk", version = "0.26.1")
  testImplementation(group = "com.squareup.okhttp3", name="mockwebserver", version = "4.11.0")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
}

kotlin {
  jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(11))
  }
}

//
// Set up various Properties and Constants used by the build script
//

// Application Properties
val author: String by project
val company: String by project


//
// Configure Testing
//
tasks.named<Test>("test") {
  useJUnitPlatform()
}

tasks.withType<DokkaTask>().configureEach {
  dokkaSourceSets {
    configureEach {
      documentedVisibilities.set(setOf(DokkaConfiguration.Visibility.PUBLIC,
        DokkaConfiguration.Visibility.PROTECTED, DokkaConfiguration.Visibility.INTERNAL,
        DokkaConfiguration.Visibility.PRIVATE))
//      documentedVisibilities.set(setOf(DokkaConfiguration.Visibility.PUBLIC))
      jdkVersion.set(11)
    }
  }
}

tasks.dokkaHtml {
  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    footerMessage = "(c) 2023 Ewert Technologies"
  }

}


/**
 * Displays general build info, such as versions, key directory locations, etc.
 */
tasks.register("buildInfo") {
  group = "help"
  description = "Displays general build info, such as versions, etc."

  logger.quiet("Project: ${project.name} - ${project.description}")
  logger.quiet("Project version: ${project.version}")
  logger.quiet("Author: $author")
  logger.quiet("Company: $company")
  logger.quiet("java.version: ${JavaVersion.current()}")
}


