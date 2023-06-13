package ca.ewert.notarytoolkotlin

import java.io.File
import java.nio.file.Path

/**
 * Test Utility Functions
 *
 * Created: 2023-06-13
 * @author vewert
 */

/**
 * Convenience Method to get a [File] from a resource location.
 *
 * @param resource Name of the resource (not must start with `/`
 */
internal fun resourceToFile(resource: String): File? {
  return object {}.javaClass.getResource(resource)?.toURI()?.let { File(it) }
}

/**
 * Convenience Method to get a [Path] from a resource location.
 *
 * @param resource Name of the resource (not must start with `/`
 */
internal fun resourceToPath(resource: String): Path? {
  return object {}.javaClass.getResource(resource)?.toURI()?.let { Path.of(it) }
}