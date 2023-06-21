package ca.ewert.notarytoolkotlin.authentication

import ca.ewert.notarytoolkotlin.errors.JsonWebTokenError
import ca.ewert.notarytoolkotlin.http.json.jwt.JwtHeaderJson
import ca.ewert.notarytoolkotlin.http.json.jwt.JwtPayloadJson
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
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
 * @property privateKeyFile Private Key file `.p8` provided by Apple
 * @property tokenLifetime Lifetime of the token, should be less than 20 minutes
 *
 * @author vewert
 */
class JsonWebToken private constructor(
  private val privateKeyId: String,
  private val issuerId: String,
  private val privateKeyFile: Path,
  private val tokenLifetime: Duration = Duration.ofMinutes(15),
  issuedAtTimeParam: Instant,
  expirationTimeParam: Instant,
  signedTokenParam: String
) {

  companion object {
    fun create(
      privateKeyId: String,
      issuerId: String,
      privateKeyFile: Path,
      tokenLifetime: Duration = Duration.of(15, ChronoUnit.MINUTES)
    ): Result<JsonWebToken, JsonWebTokenError> {
      return generateJwt(
        privateKeyId,
        issuerId,
        privateKeyFile,
        Instant.now(),
        Instant.now().plus(tokenLifetime)
      ).map { jwtString ->
        JsonWebToken(
          privateKeyId,
          issuerId,
          privateKeyFile,
          tokenLifetime,
          Instant.now(),
          Instant.now().plus(tokenLifetime),
          jwtString
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
    issuedAtTime = issuedAtTimeParam
    log.info { "Issued: $issuedAtTime" }
    expirationTime = expirationTimeParam
    log.info { "Expiry: $expirationTime" }
    signedToken = signedTokenParam
  }

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
   * @return Returns the value of the updated token String if successful or an [JsonWebTokenError]
   */
  internal fun updateWebToken(): Result<String, JsonWebTokenError> {
    issuedAtTime = Instant.now()
    expirationTime = issuedAtTime.plus(tokenLifetime)
    return generateJwt(privateKeyId, issuerId, privateKeyFile, issuedAtTime, expirationTime).map { jwtString ->
      signedToken = jwtString
      jwtString
    }
  }

  /**
   * Returns `"decodedHeader, decodedPackage"`, suitable for debugging
   */
  override fun toString(): String {
    return "${decodedHeaderJson?.toJsonString() ?: ""}, ${decodedPayloadJson?.toJsonString() ?: ""}"
  }
}