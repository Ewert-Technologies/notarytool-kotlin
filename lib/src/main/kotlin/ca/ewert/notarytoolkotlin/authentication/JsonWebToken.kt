package ca.ewert.notarytoolkotlin.authentication

import mu.KotlinLogging
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Base64


/** Logging Object */
private val log = KotlinLogging.logger {}

/** Constant for `aud` field value in JWT */
private const val AUDIENCE: String = "appstoreconnect-v1"

/**
 * This class represents a [JSON Web Token](https://jwt.io/introduction), which is required for making
 * API calls to Apples Notary Web API.
 *
 * @param privateKeyId Apple private key ID
 * @param issuerId Apple Issuer ID
 *
 * Created: 2023-06-08
 * @author vewert
 */
class JsonWebToken internal constructor(
  private val privateKeyId: String, private val issuerId: String,
  private val privateKeyFile: Path,
  private val tokenLifetime: Duration = Duration.ofMinutes(20)
) {

  /**
   * The JSON Web Token as an encoding String, suitable for use with the Apple Notary API
   */
  internal var jwtEncodedString: String? = null
    private set
    get() {
      if (isExpired) {
        log.info { "Updated Web Token" }
        updateWebToken()
      }
      return field
    }


  /**
   * The issued at Timestamp, set to current time during initialization
   */
  internal lateinit var issuedAtTime: ZonedDateTime
    private set

  /**
   * The expiration Timestamp
   */
  internal lateinit var expirationTime: ZonedDateTime
    private set

  /**
   * Checks if the current Web Token is expired
   */
  internal val isExpired: Boolean
    get() {
      log.info { "Now: ${ZonedDateTime.now()}" }
      log.info { "Expiration Date: $expirationTime" }
      return ZonedDateTime.now().isAfter(expirationTime)
    }

  /**
   * Returns the Decoded Header portion of the Jason Web Token.
   * Returns an empty string if the Jason Web Token is `null`.
   */
  internal val decodedHeader: String
    get() {
      return if (jwtEncodedString != null) {
        val jwtParts = jwtEncodedString!!.split(".")
        if (jwtParts.size == 3) {
          String(Base64.getDecoder().decode(jwtParts[0]), StandardCharsets.UTF_8)
        } else {
          ""
        }
      } else {
        ""
      }
    }

  /**
   * Returns the Decoded Payload portion of the Jason Web Token.
   * Returns an empty string if the Jason Web Token is `null`.
   */
  internal val decodedPayload: String
    get() {
      return if (jwtEncodedString != null) {
        val jwtParts = jwtEncodedString!!.split(".")
        if (jwtParts.size == 3) {
          String(Base64.getDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8)
        } else {
          ""
        }
      } else {
        ""
      }
    }

  /**
   * Initializes a JsonWebToken object.
   */
  init {
    updateWebToken()
  }

  /**
   * Generates the Web Token and stores it as a String.
   */
  private fun generateWebToken() {
    jwtEncodedString = generateJwt2(privateKeyId, issuerId, privateKeyFile, issuedAtTime, expirationTime)
  }

  /**
   * Updates the Web Token, by updated the issuedTime and expiryTime and then re-generating
   * the token.
   */
  internal fun updateWebToken() {
    issuedAtTime = ZonedDateTime.now()
    expirationTime = issuedAtTime.plus(tokenLifetime)
    generateWebToken()
  }

  /**
   * Returns `"decodedHeader, decodedPackage"`, suitable for debugging
   */
  override fun toString(): String {
    return "$decodedHeader, $decodedPayload"
  }
}