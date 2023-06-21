package ca.ewert.notarytoolkotlin.authentication

import arrow.core.getOrElse
import ca.ewert.notarytoolkotlin.http.json.jwt.JwtHeaderJson
import ca.ewert.notarytoolkotlin.http.json.jwt.JwtPayloadJson
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.getOrElse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import mu.KotlinLogging
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*


/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Moshi json parser
 */
private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

/**
 * Created: 2023-06-08
 * This class represents a [JSON Web Token](https://jwt.io/introduction), configured for making
 * API calls to Apples Notary Web API.
 *
 * @property privateKeyId Apple private key ID
 * @property issuerId Apple Issuer ID
 * @property privateKeyFile Private Key file `.p8` provided by Apple
 * @property tokenLifetime Lifetime of the token, should be less than 20 minutes
 *
 * @author vewert
 */
class JsonWebToken internal constructor(
  private val privateKeyId: String, private val issuerId: String,
  private val privateKeyFile: Path,
  private val tokenLifetime: Duration = Duration.ofMinutes(15)
) {

  /**
   * The JSON Web Token as an encoding String, suitable for use with the Apple Notary API
   */
  internal var jwtEncodedString: String? = null
    private set


  /**
   * The issued at Timestamp, set to current time during initialization
   */
  internal lateinit var issuedAtTime: Instant
    private set

  /**
   * The expiration Timestamp
   */
  internal lateinit var expirationTime: Instant
    private set

  /**
   * Checks if the current Web Token is expired
   */
  internal val isExpired: Boolean
    get() {
      log.info { "Now: ${ZonedDateTime.now()}" }
      log.info { "Expiration Date: ${expirationTime.atZone(ZoneId.systemDefault())}" }
      return Instant.now().isAfter(expirationTime)
    }

  /**
   * Returns the Decoded Header portion of the Jason Web Token.
   */
  internal val decodedHeaderJson: JwtHeaderJson?
    get() {
      return if (jwtEncodedString != null) {
        val jwtParts = jwtEncodedString!!.split(".")
        if (jwtParts.size == 3) {
          val jsonString = String(Base64.getDecoder().decode(jwtParts[0]), StandardCharsets.UTF_8)
          val jsonAdapter: JsonAdapter<JwtHeaderJson> = moshi.adapter(JwtHeaderJson::class.java)
          jsonAdapter.fromJson(jsonString)
        } else {
          null
        }
      } else {
        null
      }
    }

  /**
   * Returns the Decoded Header portion of the Jason Web Token.
   */
  internal val decodedPayloadJson: JwtPayloadJson?
    get() {
      return if (jwtEncodedString != null) {
        val jwtParts = jwtEncodedString!!.split(".")
        if (jwtParts.size == 3) {
          val jsonString = String(Base64.getDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8)
          val jsonAdapter: JsonAdapter<JwtPayloadJson> = moshi.adapter(JwtPayloadJson::class.java)
          jsonAdapter.fromJson(jsonString)
        } else {
          null
        }
      } else {
        null
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
    jwtEncodedString = generateJwt(privateKeyId, issuerId, privateKeyFile, issuedAtTime, expirationTime).getOrElse{ "" } //FIXME
  }

  /**
   * Updates the Web Token, by updated the issuedTime and expiryTime and then re-generating
   * the token.
   */
  internal fun updateWebToken() {
    issuedAtTime = Instant.now()
    expirationTime = issuedAtTime.plus(tokenLifetime)
    generateWebToken()
  }

  /**
   * Returns `"decodedHeader, decodedPackage"`, suitable for debugging
   */
  override fun toString(): String {
    return "${decodedHeaderJson?.toJsonString() ?: ""}, ${decodedPayloadJson?.toJsonString() ?: ""}"
  }
}