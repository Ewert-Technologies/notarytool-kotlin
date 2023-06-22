package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.fail
import ca.ewert.notarytoolkotlin.errors.JsonWebTokenError
import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import java.nio.file.Path

private val log = KotlinLogging.logger {}

/**
 * TODO: Add Comments
 * <br/>
 * Created: 2023-06-19
 * @author vewert
 */
class NotaryToolClientTests {

  private val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

  @Test
  fun test1() {

    val notaryToolClient = NotaryToolClient(
      privateKeyId = TestValuesReader().getKeyId(),
      issuerId = TestValuesReader().getIssueId(),
      privateKeyFile = privateKeyFile!!,
    )

    when (val submissionListResponseResult = notaryToolClient.getPreviousSubmissions()) {
      is Ok -> {
        val submissionListResponse = submissionListResponseResult.value
        assertThat(submissionListResponse.submissionInfoList).hasSize(2)
      }

      is Err -> {
        when (val error = submissionListResponseResult.error) {
          is JsonWebTokenError.TokenCreationError -> log.warn { error.msg }
          is NotaryToolError.HttpError -> log.warn { "An HTTP Error occurred. Code: ${error.httpStatusCode} - ${error.httpStatusMsg}, for request to: ${error.requestUrl}" }
          else -> log.warn { error.msg }
        }
        fail(AssertionError("Request failed with: ${submissionListResponseResult.error}"))
      }
    }

  }

}