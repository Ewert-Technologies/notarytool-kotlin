package ca.ewert.notarytoolkotlin.authentication

import assertk.assertThat
import assertk.assertions.*
import ca.ewert.notarytoolkotlin.isCloseTo
import ca.ewert.notarytoolkotlin.isOk
import ca.ewert.notarytoolkotlin.resourceToPath
import com.github.michaelbull.result.onSuccess
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [JsonWebToken] class
 *
 * Created: 2023-06-09
 * @author vewert
 */
class JsonWebTokenTest {

  /**
   * Called before each test
   */
  @BeforeEach
  fun setUp() {
  }

  /**
   * Called after each test
   */
  @AfterEach
  fun tearDown() {
  }

  /**
   * Basic test of creating a [JsonWebToken] object
   */
  @Test
  fun basicInitTest() {
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    val tokenLifetime: Duration = Duration.ofMinutes(5)

    val jsonWebTokenResult = JsonWebToken.create(
      privateKeyId = "ABCDEFG",
      issuerId = "1234567",
      privateKeyFile = privateKeyFile!!,
      tokenLifetime = tokenLifetime
    )
    assertThat(jsonWebTokenResult).isOk()

    jsonWebTokenResult.onSuccess { jsonWebToken ->
      assertThat(jsonWebToken.issuedAtTime).isCloseTo(Instant.now(), Duration.of(500, ChronoUnit.MILLIS))

      val expectedExpirationTime = Instant.now().plus(tokenLifetime)
      assertThat(jsonWebToken.expirationTime).isCloseTo(
        expected = expectedExpirationTime,
        tolerance = Duration.of(500, ChronoUnit.MILLIS)
      )
      assertThat(jsonWebToken.isExpired).isFalse()
      assertThat(jsonWebToken.signedToken).isNotEmpty()

      log.info { "Issued: ${jsonWebToken.issuedAtTime.truncatedTo(ChronoUnit.SECONDS)}" }
      val iat: String = (jsonWebToken.issuedAtTime.truncatedTo(ChronoUnit.SECONDS).epochSecond).toString()
      log.info { "iat: $iat" }

      val exp: String = (jsonWebToken.expirationTime.truncatedTo(ChronoUnit.SECONDS).epochSecond).toString()
      log.info { "exp: $exp" }

      val jwtPayload = jsonWebToken.decodedPayloadJson?.toJsonString()
      log.info { "jwtPayload: $jwtPayload" }
      val expectedPayloadString = """
        {"aud":"appstoreconnect-v1","exp":$exp,"iat":$iat,"iss":"1234567","scope":["GET /notary/v2/submissions"]}
        """.trimIndent()
      assertThat(jwtPayload).isEqualTo(expectedPayloadString)

      val jwtHeader = jsonWebToken.decodedHeaderJson?.toJsonString()
      log.info { "jwtHeader: $jwtHeader" }
      val expectedHeader = """
        {"alg":"ES256","kid":"ABCDEFG","typ":"JWT"}
        """.trimIndent()
      assertThat(jwtHeader).isEqualTo(expectedHeader)
    }
  }

  /**
   * Tests that the [JsonWebToken] expires correctly
   */
  @Test
  fun expiryTest() {
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    val tokenLifetime: Duration = Duration.ofMinutes(1)

    val jsonWebTokenResult = JsonWebToken.create(
      privateKeyId = "ABCDEFG",
      issuerId = "1234567",
      privateKeyFile = privateKeyFile!!,
      tokenLifetime = tokenLifetime
    )

    assertThat(jsonWebTokenResult).isOk()

    jsonWebTokenResult.onSuccess { jsonWebToken ->
      assertThat(jsonWebToken.issuedAtTime).isCloseTo(Instant.now(), Duration.of(500, ChronoUnit.MILLIS))
      val expectedExpirationTime = Instant.now().plus(tokenLifetime)
      assertThat(jsonWebToken.expirationTime).isCloseTo(
        expected = expectedExpirationTime,
        tolerance = Duration.of(500, ChronoUnit.MILLIS)
      )
      assertThat(jsonWebToken.isExpired).isFalse()
      Thread.sleep(75000)
      assertThat(jsonWebToken.isExpired).isTrue()
    }
  }

  /**
   * Tests that the [JsonWebToken] can be updated
   */
  @Test
  fun updateTest() {
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    val tokenLifetime: Duration = Duration.ofMinutes(1)

    val jsonWebTokenResult = JsonWebToken.create(
      privateKeyId = "ABCDEFG",
      issuerId = "1234567",
      privateKeyFile = privateKeyFile!!,
      tokenLifetime = tokenLifetime
    )

    assertThat(jsonWebTokenResult).isOk()

    jsonWebTokenResult.onSuccess { jsonWebToken ->
      assertThat(jsonWebToken.issuedAtTime).isCloseTo(Instant.now(), Duration.of(500, ChronoUnit.MILLIS))

      val expectedExpirationTime = Instant.now().plus(tokenLifetime)
      assertThat(jsonWebToken.expirationTime).isCloseTo(
        expected = expectedExpirationTime,
        tolerance = Duration.of(500, ChronoUnit.MILLIS)
      )

      assertThat(jsonWebToken.isExpired).isFalse()
      Thread.sleep(75000)
      assertThat(jsonWebToken.isExpired).isTrue()

      val updateResult = jsonWebToken.updateWebToken()
      assertThat(updateResult).isOk()

      assertThat(jsonWebToken.issuedAtTime).isCloseTo(Instant.now(), Duration.of(500, ChronoUnit.MILLIS))
      val updatedExpectedExpirationTime = Instant.now().plus(tokenLifetime)
      assertThat(jsonWebToken.expirationTime).isCloseTo(
        expected = updatedExpectedExpirationTime,
        tolerance = Duration.of(500, ChronoUnit.MILLIS)
      )
      assertThat(jsonWebToken.isExpired).isFalse()
      assertThat(jsonWebToken.decodedPayloadJson?.iat).isEqualTo(jsonWebToken.issuedAtTime.epochSecond.toInt())
      assertThat(jsonWebToken.decodedPayloadJson?.exp).isEqualTo(jsonWebToken.expirationTime.epochSecond.toInt())
    }
  }
}