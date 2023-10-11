package ca.ewert.notarytoolkotlin.examples.kotlin

import ca.ewert.notarytoolkotlin.NotaryToolClient
import ca.ewert.notarytoolkotlin.NotaryToolError
import ca.ewert.notarytoolkotlin.TestValuesReader
import ca.ewert.notarytoolkotlin.resourceToPath
import ca.ewert.notarytoolkotlin.response.SubmissionId
import ca.ewert.notarytoolkotlin.response.SubmissionLogUrlResponse
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.mapBoth
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
 * Example of getting the status log for a submission.
 */
fun main() {
  val testValuesReader = TestValuesReader()
  val keyId: String = testValuesReader.getKeyId()
  val issuerId: String = testValuesReader.getIssueId()
  val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

  val sId: SubmissionId = SubmissionId.of("").mapBoth({ it }, {
    println("hello world")
    return
  })

  val s: SubmissionId = SubmissionId.of("").getOrThrow { throw Exception("Bad") }

  val submissionIdResult: Result<SubmissionId, NotaryToolError.UserInputError.MalformedSubmissionIdError> =
    SubmissionId.of("c6da5f3b-e467-4197-98fa-c83bac3d2953")

  submissionIdResult.mapBoth(
    { submissionId: SubmissionId ->
      if (privateKeyFile != null) {
        val notaryToolClient = NotaryToolClient(
          privateKeyId = keyId,
          issuerId = issuerId,
          privateKeyFile = privateKeyFile,
        )
        val result: Result<SubmissionLogUrlResponse, NotaryToolError> = notaryToolClient.getSubmissionLog(submissionId)
        result.onSuccess { submissionLogUrlResponse ->
          println("Submission log for $submissionId: ${submissionLogUrlResponse.developerLogUrlString}")
        }
        result.onFailure { notaryToolError ->
          println(notaryToolError.longMsg)
        }
      } else {
        println("Couldn't find file")
      }
    },
    { malformedSubmissionIdError: NotaryToolError.UserInputError.MalformedSubmissionIdError ->
      println(malformedSubmissionIdError.longMsg)
    },
  )
}
