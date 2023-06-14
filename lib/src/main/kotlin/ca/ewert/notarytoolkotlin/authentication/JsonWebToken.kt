package ca.ewert.notarytoolkotlin.authentication

import ca.ewert.notarytoolkotlin.http.json.JwtHeaderJson
import ca.ewert.notarytoolkotlin.http.json.JwtPayloadJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

/**
 * Created: 2023-06-08
 * This class represents a [JSON Web Token](https://jwt.io/introduction), which is required for making
 * API calls to Apples Notary Web API.
 *
 * @property privateKeyId Apple private key ID
 * @property issuerId Apple Issuer ID
 * @property privateKeyFile Private Key file `.p8' provided by Apple
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
   */
  @OptIn(ExperimentalStdlibApi::class)
  internal val decodedHeaderJson: JwtHeaderJson?
    get() {
      return if (jwtEncodedString != null) {
        val jwtParts = jwtEncodedString!!.split(".")
        if (jwtParts.size == 3) {
          val jsonString = String(Base64.getDecoder().decode(jwtParts[0]), StandardCharsets.UTF_8)
          val jsonAdapter: JsonAdapter<JwtHeaderJson> = moshi.adapter<JwtHeaderJson>()
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
  @OptIn(ExperimentalStdlibApi::class)
  internal val decodedPayloadJson: JwtPayloadJson?
    get() {
      return if (jwtEncodedString != null) {
        val jwtParts = jwtEncodedString!!.split(".")
        if (jwtParts.size == 3) {
          val jsonString = String(Base64.getDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8)
          val jsonAdapter: JsonAdapter<JwtPayloadJson> = moshi.adapter<JwtPayloadJson>()
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
    return "${decodedHeaderJson?.toString() ?: ""}, ${decodedPayloadJson?.toString() ?: ""}"
  }
}