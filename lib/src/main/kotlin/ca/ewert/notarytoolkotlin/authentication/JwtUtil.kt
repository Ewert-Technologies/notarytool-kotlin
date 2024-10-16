package ca.ewert.notarytoolkotlin.authentication

import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError
import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError.PrivateKeyNotFoundError
import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError.TokenCreationError
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.useLines

/** Logger Object **/
private val log: KLogger = KotlinLogging.logger {}

/** Constant for the `aud` claim name in the JWT */
private const val AUDIENCE_CLAIM_NAME = "aud"

/** Constant for `aud` claim value in the JWT */
private const val AUDIENCE_CLAIM_VALUE: String = "appstoreconnect-v1"

/** Constant for the `scope` claim name in the JWT */
private const val SCOPE_CLAIM_NAME: String = "scope"

/**
 * Enum of possible scope values
 */
internal enum class Scope(val scopeValue: String) {

  /**
   * Scope value of `GET /notary/v2/submissions`
   */
  GET_SUBMISSIONS("GET /notary/v2/submissions"),
  ;

  /**
   * To String method for the Enum. Returns the [scopeValue]
   */
  override fun toString(): String {
    return scopeValue
  }
}

/**
 * Generates a Json Web Token [https://jwt.io/] suitable to add to the Header when sending a request
 * to Apple's Notary Web API. The token String is included in the request Header as:
 * `Authorization: Bearer <json web token>`,
 * e.g. `Authorization: Bearer eyJhbGciOiAiRVMyNTYiLCJraWQiOiAiMlg5UjRIWEYzNCIsInR5cCI6ICJKV1QifQ.eyJpc3MiOiAiNTcyNDY1NDItOTZmZS0xYTYzLWUwNTMtMDgyNGQwMTEwNzJhIiwiaWF0IjogMTUyODQwNzYwMCwiZXhwIjogMTUyODQwODgwMCwiYXVkIjogImFwcHN0b3JlY29ubmVjdC12MSIsInNjb3BlIjogWyJHRVQgL3YxL2FwcHM/ZmlsdGVyW3BsYXRmb3JtXT1JT1MiXX0.5kFT-fy_5izumLsy0mmVxu8lZt7K4_NoChIZzhbnBtVfbs0eLWEvq4OL5EOin17v2FepyOe8Jw1jgXlgPhmvge`
 *
 * The Json Web Token String can be useful for testing the Notary API directly, using
 * curl or an online web service tester.
 *
 * @return Either the Json Web Token String, or [JsonWebTokenError] containing an error message
 */
fun generateJwt(
  privateKeyId: String,
  issuerId: String,
  privateKeyProvider: () -> Result<ECPrivateKey, JsonWebTokenError>,
  issuedDate: Instant,
  expiryDate: Instant,
): Result<String, JsonWebTokenError> {
  return privateKeyProvider().flatMap {
    val algorithm = Algorithm.ECDSA256(it)
    val scopeArray = arrayOf(Scope.GET_SUBMISSIONS.scopeValue)
    try {
      val renderedToken: String = com.auth0.jwt.JWT.create()
        .withIssuer(issuerId)
        .withKeyId(privateKeyId)
        .withIssuedAt(issuedDate)
        .withExpiresAt(expiryDate)
        .withClaim(AUDIENCE_CLAIM_NAME, AUDIENCE_CLAIM_VALUE)
        .withArrayClaim(SCOPE_CLAIM_NAME, scopeArray)
        .sign(algorithm)
      Ok(renderedToken)
    } catch (illegalArgumentException: IllegalArgumentException) {
      log.warn(illegalArgumentException) { "Error creating JWT" }
      Err(TokenCreationError(illegalArgumentException.localizedMessage))
    } catch (jwtCreationException: JWTCreationException) {
      log.warn(jwtCreationException) { "Error creating JWT" }
      Err(TokenCreationError(jwtCreationException.localizedMessage))
    }
  }
}

/**
 * Creates an [ECPrivateKey] from the Private Key File passed in. The
 * File must be a `.p8` file containing a Base64 encoded Private Key. The Private Key String is
 * first Base64 decoded and then used to create a [PKCS8EncodedKeySpec]. The [EncodedKeySpec] is then used by
 * a [KeyFactory] to generate the Private Key, using the *"EC"* algorithm.
 *
 * @param privateKeyFile A Private Key file (`.p8`), provided by Apple
 * @return The generated Private Key, suitable for signing the JWT or an [JsonWebTokenError.PrivateKeyNotFoundError]
 */
internal fun createPrivateKey(privateKeyFile: Path): Result<ECPrivateKey, JsonWebTokenError> {
  return parsePrivateKeyString(privateKeyFile = privateKeyFile).andThen { privateKeyFileString: String ->
    createPrivateKey(
      privateKeyFileString,
    )
  }.map { ecPrivateKey -> ecPrivateKey }
}

/**
 * Parses out the Private Key String, from a `.p8` file passed in. It strips off the
 * `"-----BEGIN PRIVATE KEY-----"` and `"-----END PRIVATE KEY-----"` from the beginning and end,
 * and returns Private Key section, with line endings removed.
 * @param privateKeyFile A Private Key file (`.p8`), provided by Apple
 * @return The Private Key [String] or a [JsonWebTokenError.PrivateKeyNotFoundError]
 */
internal fun parsePrivateKeyString(privateKeyFile: Path): Result<String, JsonWebTokenError> {
  return if (privateKeyFile.exists()) {
    Ok(
      privateKeyFile.useLines { lines ->
        lines.filter { !it.matches(Regex("-*\\w+ PRIVATE KEY-*")) }.joinToString(separator = "")
      },
    )
  } else {
    Err(PrivateKeyNotFoundError(privateKeyFile))
  }
}

/**
 * Creates an [ECPrivateKey] from the Private Key String passed in. The
 * String must be a Base64 encoded Private Key. The String is first Base64 decoded
 * and then used to create a [PKCS8EncodedKeySpec]. The [EncodedKeySpec] is then used by
 * a [KeyFactory] to generate the Private Key, using the *EC* algorithm.
 *
 * @param keyString The private key as a Base64 String, e.g. from a `.p8`
 * @return The generate Private Key, suitable for signing the JWT.
 */
internal fun createPrivateKey(keyString: String): Result<ECPrivateKey, JsonWebTokenError> {
  val keyBytes: ByteArray = keyString.toByteArray()
  log.debug { "keyBytes: $ keyBytes" }

  return try {
    val keyBytesBase64Decoded: ByteArray = Base64.getDecoder().decode(keyBytes)
    log.debug { "keyBytesBase64String: $keyBytesBase64Decoded" }
    val pkcS8EncodedKeySpec = PKCS8EncodedKeySpec(keyBytesBase64Decoded)
    Ok(KeyFactory.getInstance("EC").generatePrivate(pkcS8EncodedKeySpec) as ECPrivateKey)
  } catch (exception: Exception) {
    log.warn(exception) { "Error creating ECPrivateKey" }
    Err(
      JsonWebTokenError
        .InvalidPrivateKeyError(exceptionMsg = exception.message ?: "N/A"),
    )
  }
}
