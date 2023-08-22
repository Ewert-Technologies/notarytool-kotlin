package ca.ewert.notarytoolkotlin.examples.kotlin

import ca.ewert.notarytoolkotlin.NotaryToolClient
import ca.ewert.notarytoolkotlin.NotaryToolError
import ca.ewert.notarytoolkotlin.TestValuesReader
import ca.ewert.notarytoolkotlin.resourceToPath
import ca.ewert.notarytoolkotlin.response.AwsUploadData
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import java.nio.file.Path

/**
 * Example of submitting and uploading a software file to be notarized.
 */
fun main() {
  val testValuesReader = TestValuesReader()
  val keyId: String = testValuesReader.getKeyId()
  val issuerId: String = testValuesReader.getIssueId()
  val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")
  val softwareFile: Path? = resourceToPath("/pwm_invalid_aarch64.dmg")

  if (privateKeyFile != null && softwareFile != null) {
    val notaryToolClient = NotaryToolClient(
      privateKeyId = keyId,
      issuerId = issuerId,
      privateKeyFile = privateKeyFile,
    )

    val result: Result<AwsUploadData, NotaryToolError> = notaryToolClient.submitAndUploadSoftware(softwareFile)

    result.onSuccess { awsUploadData ->
      println("Uploaded file: ${softwareFile.fileName}, and received submissionId: ${awsUploadData.submissionId}")
    }

    result.onFailure { notaryToolError ->
      println(notaryToolError.longMsg)
    }
  } else {
    println("Couldn't find file")
  }
}
