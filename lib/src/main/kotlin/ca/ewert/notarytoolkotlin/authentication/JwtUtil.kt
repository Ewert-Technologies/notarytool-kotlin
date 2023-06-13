package ca.ewert.notarytoolkotlin.authentication

import arrow.core.Either
import io.github.nefilim.kjwt.JWT
import io.github.nefilim.kjwt.sign
import mu.KLogger
import mu.KotlinLogging
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.io.path.useLines

/** Logger Object **/
private val log: KLogger = KotlinLogging.logger {}

/** Constant for `aud` field value in JWT */
private const val AUDIENCE: String = "appstoreconnect-v1"

/** Constant for the UTC Time Zone */
private val ZONE_UTC: ZoneId = ZoneId.of("UTC")


/**
 * Generates a Json Web Token [https://jwt.io/] suitable to add to the Header when sending a request
 * to Apple's Notary Web API. The token String is included in the request Header as:
 * `Authorization: Bearer <json web token>`
 */
fun generateJwt(privateKeyId: String, issuerId: String, privateKeyFile: Path, tokenLifetime: Duration): String {
  val jwt = JWT.es256(privateKeyId) {
    issuer(issuerId)
    issuedAt(LocalDateTime.ofInstant(Instant.now(), ZONE_UTC))
    expiresAt(LocalDateTime.ofInstant(Instant.now(), ZONE_UTC).plus(tokenLifetime))
    claim("aud", AUDIENCE)
  }

  val ecPrivateKey = createPrivateKey(privateKeyFile)

  return when (val result = jwt.sign(ecPrivateKey)) {
    is Either.Left -> result.value.toString()
    is Either.Right -> result.value.rendered
  }
}

/**
 * Creates an [ECPrivateKey] from the Private Key File passed in. The
 * File must be a `.p8` file containing a Base64 encoded Private Key. The Private Key String is
 * first Base64 decoded and then used to create a [PKCS8EncodedKeySpec]. The [EncodedKeySpec] is then used by
 * a [KeyFactory] to generate the Private Key, using the *"EC"* algorithm.
 *
 * @param privateKeyFile A Private Key file (`.p8`), provided by Apple
 * @return The generated Private Key, suitable for signing the JWT.
 */
internal fun createPrivateKey(privateKeyFile: Path): ECPrivateKey {
  val privateKeyString = parsePrivateKeyString(privateKeyFile = privateKeyFile)
  return createPrivateKey(privateKeyString)
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