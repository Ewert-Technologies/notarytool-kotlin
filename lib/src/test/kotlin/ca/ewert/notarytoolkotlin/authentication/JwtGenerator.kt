package ca.ewert.notarytoolkotlin.authentication

import assertk.assertThat
import assertk.assertions.isNotNull
import ca.ewert.notarytoolkotlin.TestValuesReader
import ca.ewert.notarytoolkotlin.isOk
import ca.ewert.notarytoolkotlin.resourceToPath
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Test used to generate a jwt, that can be for testing with an outside source
 * such as `curl` or [reqbin](https://app.reqbin.com)
 *
 * @author vewert
 */
class JwtGenerator {

  /**
   * Generates and logs a JWT String
   */
  @Test
  @Tag("Private")
  fun generateJwt() {
    val testValuesReader = TestValuesReader()
    val keyId: String = testValuesReader.getKeyId()
    val issuerId: String = testValuesReader.getIssueId()
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    if (privateKeyFile != null) {
      val generateJwtResult = generateJwt(
        keyId,
        issuerId,
        privateKeyFile,
        Instant.now(),
        Instant.now().plus(Duration.ofMinutes(15))
      )
      assertThat(generateJwtResult).isOk()
      generateJwtResult.onSuccess { jwtString -> log.info { "JWT: [$jwtString]" }}
    }
  }
}