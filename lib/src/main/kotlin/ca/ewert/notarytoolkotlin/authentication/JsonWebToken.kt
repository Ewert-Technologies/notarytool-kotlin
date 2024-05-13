package ca.ewert.notarytoolkotlin.authentication

import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError
import ca.ewert.notarytoolkotlin.json.jwt.JwtHeaderJson
import ca.ewert.notarytoolkotlin.json.jwt.JwtPayloadJson
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.charset.StandardCharsets
import java.security.interfaces.ECPrivateKey
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
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
 * @property privateKeyProvider Function that provides an [ECPrivateKey], used in generating the jwt.
 * @property tokenLifetime Lifetime of the token, should be less than 20 minutes
 *
 * @author vewert
 */
internal class JsonWebToken private constructor(
  private val privateKeyId: String,
  private val issuerId: String,
  private val privateKeyProvider: () -> Result<ECPrivateKey, JsonWebTokenError>,
  private val tokenLifetime: Duration = Duration.ofMinutes(15),
  _issuedAtTime: Instant,
  _expirationTime: Instant,
  _signedToken: String,
) {

  internal companion object {
    /**
     * Factor function to create an Instance of [JsonWebToken] that returns
     * a Result containing a [JsonWebToken] or a [JsonWebTokenError]
     *
     * @param privateKeyId Private Key ID, provided by Apple
     * @param issuerId Team Issuer ID provided by Apple
     * @param privateKeyProvider Function that provides an [ECPrivateKey], used in generating the jwt.
     * @param tokenLifetime Lifetime for the token, should be less than 20 minutes,
     * default value is 15 minutes
     *
     * @return An instance of [JsonWebToken] or a [JsonWebTokenError]
     */
    @JvmStatic
    fun create(
      privateKeyId: String,
      issuerId: String,
      privateKeyProvider: () -> Result<ECPrivateKey, JsonWebTokenError>,
      tokenLifetime: Duration = Duration.of(15, ChronoUnit.MINUTES),
    ): Result<JsonWebToken, JsonWebTokenError> {
      val issued = Instant.now()
      val expiry = issued.plus(tokenLifetime)
      return generateJwt(
        privateKeyId,
        issuerId,
        privateKeyProvider,
        issued,
        expiry,
      ).map { jwtString ->
        JsonWebToken(
          privateKeyId,
          issuerId,
          privateKeyProvider,
          tokenLifetime,
          issued,
          expiry,
          jwtString,
        )
      }
    }
  }

  /**
   * The issued at Timestamp, set to current time during initialization
   */
  internal var issuedAtTime: Instant
    private set

  /**
   * The expiration Timestamp, based on the [issuedAtTime] and [tokenLifetime]
   */
  internal var expirationTime: Instant
    private set

  /**
   * The signed JSON Web Token as an encoding String, suitable for use with the Apple Notary API
   */
  internal var signedToken: String
    private set

  init {
    this.issuedAtTime = _issuedAtTime
    log.debug { "Issued: ${this.issuedAtTime}" }
    this.expirationTime = _expirationTime
    log.debug { "Expiry: ${this.expirationTime}" }
    this.signedToken = _signedToken
  }

  /**
   * Checks if the current Web Token is expired
   */
  internal val isExpired: Boolean
    get() {
      log.debug { "Now: ${ZonedDateTime.now()}" }
      log.debug { "Expiration Date: ${expirationTime.atZone(ZoneId.systemDefault())}" }
      return Instant.now().isAfter(expirationTime)
    }

  /**
   * Returns the Decoded Header portion of the Jason Web Token.
   */
  internal val decodedHeaderJson: JwtHeaderJson?
    get() {
      val jwtParts = signedToken.split(".")
      return if (jwtParts.size == 3) {
        val jsonString = String(Base64.getDecoder().decode(jwtParts[0]), StandardCharsets.UTF_8)
        val jsonAdapter: JsonAdapter<JwtHeaderJson> = moshi.adapter(JwtHeaderJson::class.java)
        jsonAdapter.fromJson(jsonString)
      } else {
        null
      }
    }

  /**
   * Returns the Decoded Header portion of the Jason Web Token.
   */
  internal val decodedPayloadJson: JwtPayloadJson?
    get() {
      val jwtParts = signedToken.split(".")
      return if (jwtParts.size == 3) {
        val jsonString = String(Base64.getDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8)
        val jsonAdapter: JsonAdapter<JwtPayloadJson> = moshi.adapter(JwtPayloadJson::class.java)
        jsonAdapter.fromJson(jsonString)
      } else {
        null
      }
    }

  /**
   * Updates the Web Token by updating the issuedTime and expiryTime and then re-generating
   * the token.
   *
   * @return [JsonWebTokenError] if there is an error
   */
  internal fun updateWebToken(): Result<Unit, JsonWebTokenError> {
    issuedAtTime = Instant.now()
    expirationTime = issuedAtTime.plus(tokenLifetime)
    return generateJwt(privateKeyId, issuerId, privateKeyProvider, issuedAtTime, expirationTime).map { jwtString ->
      signedToken = jwtString
    }
  }

  /**
   * Returns `"decodedHeader, decodedPackage"`, suitable for debugging
   */
  override fun toString(): String {
    return "${decodedHeaderJson?.toJsonString() ?: ""}, ${decodedPayloadJson?.toJsonString() ?: ""}"
  }
}
