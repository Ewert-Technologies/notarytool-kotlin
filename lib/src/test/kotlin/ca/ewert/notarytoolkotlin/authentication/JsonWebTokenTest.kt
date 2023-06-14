package ca.ewert.notarytoolkotlin.authentication

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import ca.ewert.notarytoolkotlin.isCloseTo
import ca.ewert.notarytoolkotlin.resourceToPath
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime
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

    val tokenLifetime: Duration = Duration.ofMinutes(1)

    val jsonWebToken = JsonWebToken(
      privateKeyId = "ABCDEFG",
      issuerId = "1234567",
      privateKeyFile = privateKeyFile!!,
      tokenLifetime = tokenLifetime
    )

    assertThat(jsonWebToken.jwtEncodedString).isNotNull()

    assertAll {
      assertThat(jsonWebToken.issuedAtTime).isCloseTo(ZonedDateTime.now(), Duration.of(500, ChronoUnit.MILLIS))

      val expectedExpirationTime = ZonedDateTime.now().plus(tokenLifetime)
      assertThat(jsonWebToken.expirationTime).isCloseTo(
        expected = expectedExpirationTime,
        tolerance = Duration.of(500, ChronoUnit.MILLIS)
      )

      assertThat(jsonWebToken.isExpired).isFalse()
      assertThat(jsonWebToken.jwtEncodedString!!).isNotEmpty()

      val updatedTokenString: String? = jsonWebToken.jwtEncodedString
      assertThat(jsonWebToken.isExpired).isFalse()

      if (updatedTokenString != null) {
        val iat: String = (jsonWebToken.issuedAtTime.toInstant().epochSecond).toString()
        log.info { "iat: $iat" }

        val exp: String = (jsonWebToken.expirationTime.toInstant().epochSecond).toString()
        log.info { "exp: $exp" }

        val jwtPayload = jsonWebToken.decodedPayload
        log.info { "jwtPayload: $jwtPayload" }
        val expectedPayloadString = """
        {"iss":"1234567","iat":$iat,"exp":$exp,"aud":"appstoreconnect-v1","scope":["GET /notary/v2/submissions"]}
        """.trimIndent()
        assertThat(jwtPayload).isEqualTo(expectedPayloadString)
        val jwtHeader = jsonWebToken.decodedHeader
        val expectedHeader = """
        {"kid":"ABCDEFG","alg":"ES256","typ":"JWT"}
        """.trim()
        assertThat(jwtHeader).isEqualTo(expectedHeader)
      }
      assertThat(updatedTokenString).isNotNull()
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

    val jsonWebToken = JsonWebToken(
      privateKeyId = "ABCDEFG",
      issuerId = "1234567",
      privateKeyFile = privateKeyFile!!,
      tokenLifetime = tokenLifetime
    )

    assertAll {
      assertThat(jsonWebToken.issuedAtTime).isCloseTo(ZonedDateTime.now(), Duration.of(500, ChronoUnit.MILLIS))

      val expectedExpirationTime = ZonedDateTime.now().plus(tokenLifetime)
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

    val jsonWebToken = JsonWebToken(
      privateKeyId = "ABCDEFG",
      issuerId = "1234567",
      privateKeyFile = privateKeyFile!!,
      tokenLifetime = tokenLifetime
    )

    assertAll {
      assertThat(jsonWebToken.issuedAtTime).isCloseTo(ZonedDateTime.now(), Duration.of(500, ChronoUnit.MILLIS))

      val expectedExpirationTime = ZonedDateTime.now().plus(tokenLifetime)
      assertThat(jsonWebToken.expirationTime).isCloseTo(
        expected = expectedExpirationTime,
        tolerance = Duration.of(500, ChronoUnit.MILLIS)
      )

      assertThat(jsonWebToken.isExpired).isFalse()

      Thread.sleep(75000)

      jsonWebToken.updateWebToken()

      assertThat(jsonWebToken.isExpired).isFalse()
    }
  }

  /**
   * Tests that getting the web token string, automatically checks and if necessary
   * updates it.
   */
  @Test
  fun autoUpdateTest() {
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    val tokenLifetime: Duration = Duration.ofMinutes(1)

    val jsonWebToken = JsonWebToken(
      privateKeyId = "ABCDEFG",
      issuerId = "1234567",
      privateKeyFile = privateKeyFile!!,
      tokenLifetime = tokenLifetime
    )

    assertAll {
      assertThat(jsonWebToken.issuedAtTime).isCloseTo(ZonedDateTime.now(), Duration.of(500, ChronoUnit.MILLIS))

      val expectedExpirationTime = ZonedDateTime.now().plus(tokenLifetime)
      assertThat(jsonWebToken.expirationTime).isCloseTo(
        expected = expectedExpirationTime,
        tolerance = Duration.of(500, ChronoUnit.MILLIS)
      )

      assertThat(jsonWebToken.isExpired).isFalse()

      Thread.sleep(75000)
      assertThat(jsonWebToken.isExpired).isTrue()

      val updatedTokenString: String? = jsonWebToken.jwtEncodedString
      assertThat(jsonWebToken.isExpired).isFalse()

      if (updatedTokenString != null) {
        val iat: String = (jsonWebToken.issuedAtTime.toInstant().epochSecond).toString()
        log.info { "iat: $iat" }

        val exp: String = (jsonWebToken.expirationTime.toInstant().epochSecond).toString()
        log.info { "exp: $exp" }

        val jwtPayload = jsonWebToken.decodedPayload
        log.info { "jwtPayload: $jwtPayload" }
        assertThat(jwtPayload).contains("\"iat\":$iat")
        assertThat(jwtPayload).contains("\"exp\":$exp")
      }
      assertThat(updatedTokenString).isNotNull()
    }
  }
}