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
import java.nio.file.Paths

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.8.20")
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
  id("org.jmailen.kotlinter")
}

//
// Repositories for Library Dependencies
//
repositories {
  mavenCentral()
}

//
// Library Dependencies
//
dependencies {

  val moshiVersion = "1.15.0"

  // These dependencies are exported to consumers, that is to say found on their compile classpath.
  api(group = "com.michael-bull.kotlin-result", name = "kotlin-result", version = "1.1.18")

  // These dependencies are used internally, and not exposed to consumers on their own compile classpath.

  // Other
  implementation(group = "com.squareup.okhttp3", name = "okhttp", version = "4.11.0")
  implementation(group = "io.github.nefilim.kjwt", name = "kJWT", version = "0.1.6")
  implementation(group = "com.auth0", name = "java-jwt", version = "4.4.0")
  implementation(group = "com.squareup.moshi", name = "moshi", version = moshiVersion)
  implementation(group = "com.squareup.moshi", name = "moshi-adapters", version = moshiVersion)
  implementation(group = "com.squareup.moshi", name = "moshi-kotlin", version = moshiVersion)

  // Logging
  implementation(group = "io.github.oshai", name = "kotlin-logging-jvm", version = "4.0.0")
  implementation(group = "org.slf4j", name = "slf4j-api", version = "2.0.7")
  implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.4.8")

  // Testing
  testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.9.3")
  testImplementation(group = "com.willowtreeapps.assertk", name = "assertk", version = "0.26.1")
  testImplementation(group = "com.squareup.okhttp3", name = "mockwebserver", version = "4.11.0")
}

//
// Apply a specific Java toolchain to ease working on different environments.
//
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

//
// Configure ktlint
//
kotlinter {
  reporters = arrayOf("html", "json")
}

//
// Set Default Task
//
defaultTasks("release")

//
// Configure Documentation
//

/**
 * Generates Dokka Documentation in html format for all visibilities
 */
tasks.register<DokkaTask>("dokkaHtmlPrivate") {
  group = "documentation"
  description = "Generates Dokka Documentation in `html` format for all visibilities"

  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    footerMessage = "(c) 2023 Ewert Technologies"
  }

  dokkaSourceSets {
    configureEach {
      documentedVisibilities.set(
        setOf(
          DokkaConfiguration.Visibility.PUBLIC,
          DokkaConfiguration.Visibility.PROTECTED,
          DokkaConfiguration.Visibility.INTERNAL,
          DokkaConfiguration.Visibility.PRIVATE,
        )
      )
      jdkVersion.set(11)
    }
  }
}

/**
 * Generates Dokka Documentation in html format for public items
 */
tasks.register<DokkaTask>("dokkaHtmlPublic") {
  group = "documentation"
  description = "Generates Dokka Documentation in 'html' format for public items"

  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    footerMessage = "(c) 2023 Ewert Technologies"
  }

  dokkaSourceSets {
    configureEach {
      documentedVisibilities.set(
        setOf(
          DokkaConfiguration.Visibility.PUBLIC,
        )
      )
      jdkVersion.set(11)
    }
  }
}

//
// Other Tasks
//

/**
 * Displays general build info, such as versions, key directory locations, etc.
 */
tasks.register("buildInfo") {
  group = "help"
  description = "Displays general build info, such as versions, etc."

  logger.quiet("Gradle Version: ${gradle.gradleVersion}")
  logger.quiet("Project: ${project.name} - ${project.description}")
  logger.quiet("Project version: ${project.version}")
  logger.quiet("Author: $author")
  logger.quiet("Company: $company")
  logger.quiet("build dir: ${project.buildDir}")
}

/**
 * Creates a release by building and then copying the release artifacts
 * to the rel directory. Release artifacts in include the jar,
 * sources jar, and kdocs.
 */
tasks.register("release") {
  group = project.name
  description = "Creates a release"
  dependsOn("buildInfo", "build", "kotlinSourcesJar", "dokkaHtmlPublic")

  doLast {
    val relDir = file(Paths.get("rel", "${project.version}"))
    mkdir(relDir)

    copy {
      from(file(Paths.get(project.buildDir.absolutePath, "libs"))) {
        include("*.jar")
      }
      into(relDir)
    }

    copy {
      from(file(Paths.get(project.buildDir.absolutePath, "dokka", "htmlPublic")))
      into(file(Paths.get(relDir.absolutePath, "docs")))
    }

    logger.quiet("Release artifacts copied to $relDir")
  }
}
