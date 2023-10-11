package ca.ewert.notarytoolkotlin.examples.kotlin

import ca.ewert.notarytoolkotlin.NotaryToolClient
import ca.ewert.notarytoolkotlin.NotaryToolError
import ca.ewert.notarytoolkotlin.TestValuesReader
import ca.ewert.notarytoolkotlin.resourceToPath
import ca.ewert.notarytoolkotlin.response.SubmissionId
import ca.ewert.notarytoolkotlin.response.SubmissionStatusResponse
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import java.nio.file.Path

/**
 * TODO: Add Comments
 *
 * Created: 2023-08-22
 * @author Victor Ewert
 */

/**
 * Example of submitting and uploading a software file to be notarized.
 */
fun main() {
  val testValuesReader = TestValuesReader()
  val keyId: String = testValuesReader.getKeyId()
  val issuerId: String = testValuesReader.getIssueId()
  val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")
  val submissionIdResult: Result<SubmissionId, NotaryToolError.UserInputError.MalformedSubmissionIdError> =
    SubmissionId.of("c6da5f3b-e467-4197-98fa-c83bac3d2953")

  submissionIdResult.onSuccess { submissionId ->
    if (privateKeyFile != null) {
      val notaryToolClient = NotaryToolClient(
        privateKeyId = keyId,
        issuerId = issuerId,
        privateKeyFile = privateKeyFile,
      )

      val result: Result<SubmissionStatusResponse, NotaryToolError> = notaryToolClient.getSubmissionStatus(submissionId)
      result.onSuccess { submissionStatusResponse ->
        val statusInfo = submissionStatusResponse.submissionInfo
        println("Status for submission $submissionId (${statusInfo.name}): ${statusInfo.status}")
      }

      result.onFailure { notaryToolError ->
        println(notaryToolError.longMsg)
      }
    } else {
      println("Couldn't find file")
    }
  }

  submissionIdResult.onFailure { malformedSubmissionIdError ->
    println(malformedSubmissionIdError)
  }
}
