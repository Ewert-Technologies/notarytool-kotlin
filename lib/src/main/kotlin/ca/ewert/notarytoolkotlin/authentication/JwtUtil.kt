package ca.ewert.notarytoolkotlin.authentication

import ca.ewert.notarytoolkotlin.errors.JsonWebTokenError
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.github.michaelbull.result.*
import mu.KLogger
import mu.KotlinLogging
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.io.path.absolutePathString
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

/** Constant for the UTC Time Zone */
private val ZONE_UTC: ZoneId = ZoneId.of("UTC")

/**
 * Enum of possible scope values
 */
enum class Scope(val scopeValue: String) {

  /**
   * Scope value of `GET /notary/v2/submissions`
   */
  GET_SUBMISSIONS("GET /notary/v2/submissions");

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
 * `Authorization: Bearer <json web token>`
 *
 * @return Either the Json Web Token String, or [JsonWebTokenError] containing an error message
 */
fun generateJwt(
  privateKeyId: String, issuerId: String, privateKeyFile: Path,
  issuedDate: Instant, expiryDate: Instant
): Result<String, JsonWebTokenError> {

  val res = createPrivateKey(privateKeyFile)
  return res.flatMap {
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
      log.warn("Error creating JWT", illegalArgumentException)
      Err(JsonWebTokenError.TokenCreationError("Error creating JWT, provided algorithm is null"))
    } catch (jwtCreationException: JWTCreationException) {
      log.warn("Error creating JWT", jwtCreationException)
      Err(JsonWebTokenError.TokenCreationError("Error creating JWT: ${jwtCreationException.message}"))
    }
  }


//  val res = createPrivateKey(privateKeyFile)

//  createPrivateKey(privateKeyFile).mapEither(
//    { ecPrivateKey ->
//      {
//        val algorithm = Algorithm.ECDSA256(ecPrivateKey)
//        val scopeArray = arrayOf(Scope.GET_SUBMISSIONS.scopeValue)
//        try {
//          val renderedToken: String = com.auth0.jwt.JWT.create()
//            .withIssuer(issuerId)
//            .withKeyId(privateKeyId)
//            .withIssuedAt(issuedDate)
//            .withExpiresAt(expiryDate)
//            .withClaim(AUDIENCE_CLAIM_NAME, AUDIENCE_CLAIM_VALUE)
//            .withArrayClaim(SCOPE_CLAIM_NAME, scopeArray)
//            .sign(algorithm)
//          Ok(renderedToken)
//        } catch (illegalArgumentException: IllegalArgumentException) {
//          log.warn("Error creating JWT", illegalArgumentException)
//          Err(JsonWebTokenError.TokenCreationError("Error creating JWT, provided algorithm is null"))
//        } catch (jwtCreationException: JWTCreationException) {
//          log.warn("Error creating JWT", jwtCreationException)
//          Err(JsonWebTokenError.TokenCreationError("Error creating JWT: ${jwtCreationException.message}"))
//        }
//      }
//    },
//    { jsonWebTokenError -> Err(jsonWebTokenError) }
//  )

//  return when (val privateKeyResult = createPrivateKey(privateKeyFile)) {
//    is Ok -> {
//      val algorithm = Algorithm.ECDSA256(privateKeyResult.value)
//      val scopeArray = arrayOf(Scope.GET_SUBMISSIONS.scopeValue)
//      try {
//        val renderedToken: String = com.auth0.jwt.JWT.create()
//          .withIssuer(issuerId)
//          .withKeyId(privateKeyId)
//          .withIssuedAt(issuedDate)
//          .withExpiresAt(expiryDate)
//          .withClaim(AUDIENCE_CLAIM_NAME, AUDIENCE_CLAIM_VALUE)
//          .withArrayClaim(SCOPE_CLAIM_NAME, scopeArray)
//          .sign(algorithm)
//        Ok(renderedToken)
//      } catch (illegalArgumentException: IllegalArgumentException) {
//        log.warn("Error creating JWT", illegalArgumentException)
//        Err(JsonWebTokenError.TokenCreationError("Error creating JWT, provided algorithm is null"))
//      } catch (jwtCreationException: JWTCreationException) {
//        log.warn("Error creating JWT", jwtCreationException)
//        Err(JsonWebTokenError.TokenCreationError("Error creating JWT: ${jwtCreationException.message}"))
//      }
//    }
//    is Err -> privateKeyResult
//  }
}

/**
 * Creates an [ECPrivateKey] from the Private Key File passed in. The
 * File must be a `.p8` file containing a Base64 encoded Private Key. The Private Key String is
 * first Base64 decoded and then used to create a [PKCS8EncodedKeySpec]. The [EncodedKeySpec] is then used by
 * a [KeyFactory] to generate the Private Key, using the *"EC"* algorithm.
 *
 * @param privateKeyFile A Private Key file (`.p8`), provided by Apple
 * @return The generated Private Key, suitable for signing the JWT or an [JsonWebTokenError.PrivateKeyNotFound]
 */
internal fun createPrivateKey(privateKeyFile: Path): Result<ECPrivateKey, JsonWebTokenError> {
  return if (privateKeyFile.exists()) {
    val privateKeyString = parsePrivateKeyString(privateKeyFile = privateKeyFile)
    Ok(createPrivateKey(privateKeyString))
  } else {
    Err(JsonWebTokenError.PrivateKeyNotFound("Private Key File: '${privateKeyFile.absolutePathString()}' does not exist"))
  }
}


/**
 * Parses out the Private Key String, from a `.p8` file passed in. It strips off the
 * `"-----BEGIN PRIVATE KEY-----"` and `"-----END PRIVATE KEY-----"` from the beginning and end,
 * and returns Private Key section, with line endings removed.
 *
 * @param privateKeyFile A Private Key file (`.p8`), provided by Apple
 */
internal fun parsePrivateKeyString(privateKeyFile: Path): String {
  return privateKeyFile.useLines { lines ->
    lines.filter { !it.matches(Regex("-*\\w+ PRIVATE KEY-*")) }.joinToString(separator = "")
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
internal fun createPrivateKey(keyString: String): ECPrivateKey {
  val keyBytes: ByteArray = keyString.toByteArray()
  log.debug { "keyBytes: $ keyBytes" }
  val keyBytesBase64Decoded: ByteArray = Base64.getDecoder().decode(keyBytes)
  log.debug { "keyBytesBase64String: $keyBytesBase64Decoded" }
  val pkcS8EncodedKeySpec = PKCS8EncodedKeySpec(keyBytesBase64Decoded)
  return KeyFactory.getInstance("EC").generatePrivate(pkcS8EncodedKeySpec) as ECPrivateKey
}