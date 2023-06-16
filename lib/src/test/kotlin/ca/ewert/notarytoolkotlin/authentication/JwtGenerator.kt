package ca.ewert.notarytoolkotlin.authentication

import assertk.assertThat
import assertk.assertions.isNotNull
import ca.ewert.notarytoolkotlin.TestValuesReader
import ca.ewert.notarytoolkotlin.resourceToPath
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime

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
  fun generateJwt() {
    val testValuesReader = TestValuesReader()
    val keyId: String = testValuesReader.getKeyId()
    val issuerId: String = testValuesReader.getIssueId()
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    if (privateKeyFile != null) {
      val jwtString = generateJwt2(
        keyId,
        issuerId,
        privateKeyFile,
        ZonedDateTime.now(),
        ZonedDateTime.now().plus(Duration.ofMinutes(15))
      )
      log.info { "JWT: [$jwtString]" }
    }
  }
}