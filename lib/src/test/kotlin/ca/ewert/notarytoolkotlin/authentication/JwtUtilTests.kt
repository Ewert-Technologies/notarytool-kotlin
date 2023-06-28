package ca.ewert.notarytoolkotlin.authentication

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.fail
import ca.ewert.notarytoolkotlin.hasMessage
import ca.ewert.notarytoolkotlin.isErrAnd
import ca.ewert.notarytoolkotlin.isOkAnd
import ca.ewert.notarytoolkotlin.resourceToPath
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

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

    val issuedDate: ZonedDateTime = ZonedDateTime.of(2023, 6, 13, 10, 25, 12, 223331800, ZoneId.of("GMT"))
    val expiryDate: ZonedDateTime = issuedDate.plus(15, ChronoUnit.MINUTES)

    val generateJwtResult = generateJwt(
      privateKeyId = "ABCDE12345",
      issuerId = "70c5de5f-f737-47e2-e043-5b8c7c22a4d9",
      privateKeyFile = privateKeyFile!!,
      issuedDate.toInstant(),
      expiryDate.toInstant(),
    )

    generateJwtResult.onFailure { jsonWebTokenError ->
      fail(AssertionError("Generating Web Token failed: ${jsonWebTokenError.msg}"))
    }

    generateJwtResult.onSuccess { renderedJwt ->
      assertThat(renderedJwt).isNotEmpty()

      log.info { "Rendered JWT: $renderedJwt" }

      val jwtParts: List<String> = renderedJwt.split(".")
      assertThat(jwtParts).hasSize(3)

      val expectedHeader = """
      {"kid":"ABCDE12345","alg":"ES256","typ":"JWT"}
      """.trim()
      assertThat(
        String(
          Base64.getDecoder().decode(jwtParts[0]),
          StandardCharsets.UTF_8,
        ),
      ).isEqualTo(expectedHeader.trim())

      val expectedPayload = """
      {"iss":"70c5de5f-f737-47e2-e043-5b8c7c22a4d9","iat":1686651912,"exp":1686652812,"aud":"appstoreconnect-v1","scope":["GET /notary/v2/submissions"]}
      """.trim()
      assertThat(
        String(
          Base64.getDecoder().decode(jwtParts[1]),
          StandardCharsets.UTF_8,
        ),
      ).isEqualTo(expectedPayload.trim())
    }
  }

  /**
   * Tests attempting to create a rendered JWT String, with a Private Key file, that doesn't exist
   */
  @Test
  fun generateJwtTest2() {
    val privateKeyFile: Path = Paths.get("notExist.file")
    val issuedDate: ZonedDateTime = ZonedDateTime.of(2023, 6, 13, 10, 25, 0, 0, ZoneId.of("UTC"))
    val expiryDate: ZonedDateTime = issuedDate.plus(15, ChronoUnit.MINUTES)

    val generateJwtResult = generateJwt(
      privateKeyId = "ABCDE12345",
      issuerId = "70c5de5f-f737-47e2-e043-5b8c7c22a4d9",
      privateKeyFile = privateKeyFile,
      issuedDate.toInstant(),
      expiryDate.toInstant(),
    )

    val expectedMsg =
      "Private Key File: 'D:\\users\\vewert\\DevProj\\notarytool-kotlin\\lib\\notExist.file' does not exist"
    assertThat(generateJwtResult).isErrAnd().hasMessage(expectedMsg)
  }

  /**
   * Tests attempting to create a rendered JWT String, where the privateKeyId and issuerId are empty. The rendered String
   * should create successfully, but wouldn't actually be successful.
   */
  @Test
  fun generateJwtTest3() {
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    val issuedDate: ZonedDateTime = ZonedDateTime.of(2023, 6, 13, 10, 25, 0, 0, ZoneId.of("UTC"))
    val expiryDate: ZonedDateTime = issuedDate.plus(15, ChronoUnit.MINUTES)

    val generateJwtResult = generateJwt(
      privateKeyId = "",
      issuerId = "",
      privateKeyFile = privateKeyFile!!,
      issuedDate.toInstant(),
      expiryDate.toInstant(),
    )

    generateJwtResult.onFailure { jsonWebTokenError ->
      fail(AssertionError("Generating Web Token failed: ${jsonWebTokenError.msg}"))
    }

    generateJwtResult.onSuccess { renderedJwt ->
      assertThat(renderedJwt).isNotEmpty()
      val jwtParts: List<String> = renderedJwt.split(".")
      assertThat(jwtParts).hasSize(3)

      val expectedHeader = """
      {"kid":"","alg":"ES256","typ":"JWT"}
      """.trim()
      assertThat(
        String(
          Base64.getDecoder().decode(jwtParts[0]),
          StandardCharsets.UTF_8,
        ),
      ).isEqualTo(expectedHeader.trim())

      val expectedPayload = """
      {"iss":"","iat":1686651900,"exp":1686652800,"aud":"appstoreconnect-v1","scope":["GET /notary/v2/submissions"]}
      """.trim()
      assertThat(
        String(
          Base64.getDecoder().decode(jwtParts[1]),
          StandardCharsets.UTF_8,
        ),
      ).isEqualTo(expectedPayload.trim())
    }
  }

  /**
   * Tests parsing out the Private Key String, from Apples Private Key File (`.p8` file)
   * Tests case were Header and Footer parts are included.
   */
  @Test
  fun parsePrivateKeyFromFileTest1() {
    val expectedPrivateKeyString =
      "UmhvbmN1cyBuYXRvcXVlIGNvbW1vZG8gc29kYWxlcyBoZW5kcmVyaXQgcXVhbS4VyB2YXJpdXMgZmFjaWxpc2lzIG5vbiBsb3JlbS4gVmVsIGludGVyZHVtIHZlbCB0GVsZXJpc3F1ZSBpYWN1bGlzIGFlbmVhbiBtYXVyaXMgdml0YWUuIERpY3R1bSBhbE1cy4gVGF"
    val privateKeyFile = this::class.java.getResource("/privateKey1.p8")?.toURI()?.let { Paths.get(it) }
    assertThat(privateKeyFile).isNotNull()
    if (privateKeyFile != null) {
      assertThat(parsePrivateKeyString(privateKeyFile)).isOkAnd().isEqualTo(expectedPrivateKeyString)
    }
  }

  /**
   * Tests parsing out the Private Key String, from Apples Private Key File (`.p8` file)
   * Tests case were Header and Footer parts are excluded.
   */
  @Test
  fun parsePrivateKeyFromFileTest2() {
    val expectedPrivateKeyString =
      "UmhvbmN1cyBuYXRvcXVlIGNvbW1vZG8gc29kYWxlcyBoZW5kcmVyaXQgcXVhbS4VyB2YXJpdXMgZmFjaWxpc2lzIG5vbiBsb3JlbS4gVmVsIGludGVyZHVtIHZlbCB0GVsZXJpc3F1ZSBpYWN1bGlzIGFlbmVhbiBtYXVyaXMgdml0YWUuIERpY3R1bSBhbE1cy4gVGF"
    val privateKeyFile = this::class.java.getResource("/privateKey2.p8")?.toURI()?.let { Paths.get(it) }
    assertThat(privateKeyFile).isNotNull()
    if (privateKeyFile != null) {
      assertThat(parsePrivateKeyString(privateKeyFile)).isOkAnd().isEqualTo(expectedPrivateKeyString)
    }
  }

  /**
   * Tests parsing out the Private Key String, from Apples Private Key File (`.p8` file)
   * Tests case were Header and Footer parts are excluded and there are blank lines
   * at the top and bottom.
   */
  @Test
  fun parsePrivateKeyFromFileTest3() {
    val expectedPrivateKeyString =
      "UmhvbmN1cyBuYXRvcXVlIGNvbW1vZG8gc29kYWxlcyBoZW5kcmVyaXQgcXVhbS4VyB2YXJpdXMgZmFjaWxpc2lzIG5vbiBsb3JlbS4gVmVsIGludGVyZHVtIHZlbCB0GVsZXJpc3F1ZSBpYWN1bGlzIGFlbmVhbiBtYXVyaXMgdml0YWUuIERpY3R1bSBhbE1cy4gVGF"
    val privateKeyFile = this::class.java.getResource("/privateKey3.p8")?.toURI()?.let { Paths.get(it) }
    assertThat(privateKeyFile).isNotNull()
    if (privateKeyFile != null) {
      assertThat(parsePrivateKeyString(privateKeyFile)).isOkAnd().isEqualTo(expectedPrivateKeyString)
    }
  }

  /**
   * Tests passing in a file, that doesn't exist. Verifies that an Error result is returned.
   */
  @Test
  fun parsePrivateKeyFromFileTest4() {
    val expectedMsg =
      "Private Key File: 'D:\\users\\vewert\\DevProj\\notarytool-kotlin\\lib\\.\\noFile.file' does not exist"
    val privateKeyFile = Paths.get("./noFile.file")
    assertThat(parsePrivateKeyString(privateKeyFile)).isErrAnd().hasMessage(expectedMsg)
  }
}
