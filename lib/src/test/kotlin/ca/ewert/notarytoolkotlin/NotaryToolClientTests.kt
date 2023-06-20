package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.size
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

  val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

  @Test
  fun test1() {

    val notaryToolClient = NotaryToolClient(
      privateKeyId = TestValuesReader().getKeyId(),
      issuerId = TestValuesReader().getIssueId(),
      privateKeyFile = privateKeyFile!!,
    )

    val submissionListResponse = notaryToolClient.getPreviousSubmissions()
    assertThat(submissionListResponse != null)
    if (submissionListResponse != null) {
      assertThat(submissionListResponse.submissionInfo).hasSize(2)
    }
  }

}