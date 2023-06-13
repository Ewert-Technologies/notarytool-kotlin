package ca.ewert.notarytoolkotlin.authentication

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import ca.ewert.notarytoolkotlin.resourceToPath
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Base64

private val log = KotlinLogging.logger {}

/**
 * Tests JwtUtil functions
 *
 * Created: 2023-06-09
 * @author vewert
 */
class JwtUtilTests {

  /**
   * Tests creating a rendered JWT String, which can be used when making api requests.
   */
  @Test
  fun generateJwtTest1() {
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")
    assertThat(privateKeyFile).isNotNull()

    val issuedDate: ZonedDateTime = ZonedDateTime.of(2023, 6, 13, 10, 25, 0, 0, ZoneId.of("UTC"))
    val expiryDate: ZonedDateTime = issuedDate.plus(15, ChronoUnit.MINUTES)

    val renderedJwt: String = generateJwt(
      privateKeyId = "ABCDE12345",
      issuerId = "70c5de5f-f737-47e2-e043-5b8c7c22a4d9",
      privateKeyFile = privateKeyFile!!,
      issuedDate,
      expiryDate
    )

    log.info { "Rendered JWT: $renderedJwt" }
    val jwtParts: List<String> = renderedJwt.split(".")

    assertAll {
      assertThat(renderedJwt).isNotEmpty()
      assertThat(jwtParts).hasSize(3)

      val expectedHeader = """
      {"alg":"ES256","typ":"JWT","kid":"ABCDE12345"}
      """
      assertThat(String(Base64.getDecoder().decode(jwtParts[0]), StandardCharsets.UTF_8)).isEqualTo(expectedHeader.trim())

      val expectedPayload = """
      {"iss":"70c5de5f-f737-47e2-e043-5b8c7c22a4d9","iat":1686651900,"exp":1686652800,"aud":"appstoreconnect-v1"}
      """
      assertThat(String(Base64.getDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8)).isEqualTo(expectedPayload.trim())
    }
  }

  /**
   * Tests creating a rendered JWT String, which can be used when making api requests.
   */
  @Test
  fun generateJwtTest2() {
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    val issuedDate: ZonedDateTime = ZonedDateTime.of(2023, 6, 13, 10, 25, 0, 0, ZoneId.of("UTC"))
    val expiryDate: ZonedDateTime = issuedDate.plus(15, ChronoUnit.MINUTES)

    val renderedJwt: String = generateJwt2(
      privateKeyId = "ABCDE12345",
      issuerId = "70c5de5f-f737-47e2-e043-5b8c7c22a4d9",
      privateKeyFile = privateKeyFile!!,
      issuedDate,
      expiryDate
    )

    log.info { "Rendered JWT: $renderedJwt" }
    val jwtParts: List<String> = renderedJwt.split(".")

    assertAll {
      assertThat(renderedJwt).isNotEmpty()
      assertThat(jwtParts).hasSize(3)

      val expectedHeader = """
      {"kid":"ABCDE12345","alg":"ES256","typ":"JWT"}
      """
      assertThat(String(Base64.getDecoder().decode(jwtParts[0]), StandardCharsets.UTF_8)).isEqualTo(expectedHeader.trim())

      val expectedPayload = """
      {"iss":"70c5de5f-f737-47e2-e043-5b8c7c22a4d9","iat":1686651900,"exp":1686652800,"aud":"appstoreconnect-v1","scope":["GET /notary/v2/submissions"]}
      """
      assertThat(String(Base64.getDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8)).isEqualTo(expectedPayload.trim())
    }
  }

  /**
   * Tests parsing out the Private Key String, from Apples Private Key File (`.p8` file)
   * Tests case were Header and Footer parts are included.
   */
  @Test
  fun parsePrivateKeyFromFileTest1() {
    val expectedPrivateKeyString = "UmhvbmN1cyBuYXRvcXVlIGNvbW1vZG8gc29kYWxlcyBoZW5kcmVyaXQgcXVhbS4VyB2YXJpdXMgZmFjaWxpc2lzIG5vbiBsb3JlbS4gVmVsIGludGVyZHVtIHZlbCB0GVsZXJpc3F1ZSBpYWN1bGlzIGFlbmVhbiBtYXVyaXMgdml0YWUuIERpY3R1bSBhbE1cy4gVGF"
    val privateKeyFile = this::class.java.getResource("/privateKey1.p8")?.toURI()?.let { Paths.get(it) }
    assertThat(privateKeyFile).isNotNull()
    if (privateKeyFile != null) {
      val privateKeyString = parsePrivateKeyString(privateKeyFile)
      assertThat(privateKeyString).isEqualTo(expectedPrivateKeyString)
    }
  }

  /**
   * Tests parsing out the Private Key String, from Apples Private Key File (`.p8` file)
   * Tests case were Header and Footer parts are excluded.
   */
  @Test
  fun parsePrivateKeyFromFileTest2() {
    val expectedPrivateKeyString = "UmhvbmN1cyBuYXRvcXVlIGNvbW1vZG8gc29kYWxlcyBoZW5kcmVyaXQgcXVhbS4VyB2YXJpdXMgZmFjaWxpc2lzIG5vbiBsb3JlbS4gVmVsIGludGVyZHVtIHZlbCB0GVsZXJpc3F1ZSBpYWN1bGlzIGFlbmVhbiBtYXVyaXMgdml0YWUuIERpY3R1bSBhbE1cy4gVGF"
    val privateKeyFile = this::class.java.getResource("/privateKey2.p8")?.toURI()?.let { Paths.get(it) }
    assertThat(privateKeyFile).isNotNull()
    if (privateKeyFile != null) {
      val privateKeyString = parsePrivateKeyString(privateKeyFile)
      assertThat(privateKeyString).isEqualTo(expectedPrivateKeyString)
    }
  }

  /**
   * Tests parsing out the Private Key String, from Apples Private Key File (`.p8` file)
   * Tests case were Header and Footer parts are excluded and there are blank lines
   * at the top and bottom.
   */
  @Test
  fun parsePrivateKeyFromFileTest3() {
    val expectedPrivateKeyString = "UmhvbmN1cyBuYXRvcXVlIGNvbW1vZG8gc29kYWxlcyBoZW5kcmVyaXQgcXVhbS4VyB2YXJpdXMgZmFjaWxpc2lzIG5vbiBsb3JlbS4gVmVsIGludGVyZHVtIHZlbCB0GVsZXJpc3F1ZSBpYWN1bGlzIGFlbmVhbiBtYXVyaXMgdml0YWUuIERpY3R1bSBhbE1cy4gVGF"
    val privateKeyFile = this::class.java.getResource("/privateKey3.p8")?.toURI()?.let { Paths.get(it) }
    assertThat(privateKeyFile).isNotNull()
    if (privateKeyFile != null) {
      val privateKeyString = parsePrivateKeyString(privateKeyFile)
      assertThat(privateKeyString).isEqualTo(expectedPrivateKeyString)
    }
  }
}