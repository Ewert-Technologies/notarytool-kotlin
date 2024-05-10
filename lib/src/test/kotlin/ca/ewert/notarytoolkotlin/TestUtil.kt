package ca.ewert.notarytoolkotlin

import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError
import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError.PrivateKeyNotFoundError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.File
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.useLines

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

/**
 * Convenience class to create an [ECPrivateKey] from a file path.
 *
 * @param path path to the private key (`.p8`) file.
 */
internal fun privateKeyFromPath(path: Path): Result<ECPrivateKey, JsonWebTokenError> {
  return if (path.exists()) {
    val keyBytes = path.useLines { lines ->
      lines.filter { !it.matches(Regex("-*\\w+ PRIVATE KEY-*")) }.joinToString(separator = "").toByteArray()
    }
    try {
      val keyBytesBase64Decoded: ByteArray = Base64.getDecoder().decode(keyBytes)
      val pkcS8EncodedKeySpec = PKCS8EncodedKeySpec(keyBytesBase64Decoded)
      Ok(KeyFactory.getInstance("EC").generatePrivate(pkcS8EncodedKeySpec) as ECPrivateKey)
    } catch (exception: Exception) {
      Err(
        JsonWebTokenError
          .InvalidPrivateKeyError(exceptionMsg = exception.message ?: "N/A"),
      )
    }
  } else {
    Err(PrivateKeyNotFoundError(path))
  }
}
