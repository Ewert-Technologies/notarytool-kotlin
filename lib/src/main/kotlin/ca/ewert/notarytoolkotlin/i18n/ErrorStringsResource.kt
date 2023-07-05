package ca.ewert.notarytoolkotlin.i18n

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * TODO: Add Documentation
 *
 * @author Victor Ewert
 */
object ErrorStringsResource {
  private val resourceBundle: ResourceBundle = ResourceBundle.getBundle("strings/ErrorStrings")

  internal val resourceBundleName: String
    get() = resourceBundle.baseBundleName

  /**
   * Gets a string for the given key from this resource bundle. If the key cannot be found, it returns
   * a String of the format `""!-- $key --!""`
   *
   * @param key The key for the desired String
   * @return String associated with the key
   */
  fun getString(key: String): String {
    return try {
      resourceBundle.getString(key)
    } catch (e: Exception) {
      log.warn { "Invalid key used: \"$key\", for resource: $resourceBundleName" }
      "!-- $key --!"
    }
  }
}
