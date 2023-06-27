package ca.ewert.notarytoolkotlin

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import java.util.Properties

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Utility class to read test values from the resources/private/testValues.properties file
 *
 * @author vewert
 */
class TestValuesReader {

  /**
   * Properties
   */
  private val properties: Properties = Properties()

  init {
    log.info { "Inside init" }
    loadProperties()
  }

  /**
   * Loads the Properties from the file
   */
  private fun loadProperties() {
    log.info { "Inside Load Properties" }
    val propertiesFileInputStream: InputStream? =
      TestValuesReader::class.java.getResourceAsStream("/private/testValues.properties")

    if (propertiesFileInputStream != null) {
      properties.load(propertiesFileInputStream)
    }
  }

  /**
   * Reads and returns the `keyId` property from the file
   *
   * @return The value of 'keyId' property or empty String if key isn't found.
   */
  internal fun getKeyId(): String {
    return properties.getProperty("keyId", "")
  }

  /**
   * Reads and returns the `issuerId` property from the file
   *
   * @return The value of 'issuerId' property or empty String if key isn't found.
   */
  internal fun getIssueId(): String {
    return properties.getProperty("issuerId", "")
  }
}

