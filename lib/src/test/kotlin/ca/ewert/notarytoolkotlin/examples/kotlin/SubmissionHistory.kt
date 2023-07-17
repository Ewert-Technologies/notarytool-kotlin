package ca.ewert.notarytoolkotlin.examples.kotlin

import ca.ewert.notarytoolkotlin.NotaryToolClient
import ca.ewert.notarytoolkotlin.NotaryToolError
import ca.ewert.notarytoolkotlin.TestValuesReader
import ca.ewert.notarytoolkotlin.resourceToPath
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val log = KotlinLogging.logger {}

/**
 * Lists the submission history
 */
fun main(args: Array<String>) {
  val testValuesReader = TestValuesReader()
  val keyId: String = testValuesReader.getKeyId()
  val issuerId: String = testValuesReader.getIssueId()
  val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

  if (privateKeyFile != null) {
    val notaryToolClient = NotaryToolClient(
      privateKeyId = keyId,
      issuerId = issuerId,
      privateKeyFile = privateKeyFile,
    )

    when (val result = notaryToolClient.getPreviousSubmissions()) {
      is Ok -> {
        val submissionListResponse = result.value
        println(
          "Response Received on: ${
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)
              .format(submissionListResponse.receivedTimestamp.atZone(ZoneId.systemDefault()))
          }",
        )
        println("Response Status: ${submissionListResponse.responseMetaData.httpStatusString}")
        submissionListResponse.submissionInfoList.forEach { submissionInfo ->
          println("${submissionInfo.createdDate}\t${submissionInfo.name}\t${submissionInfo.id}\t${submissionInfo.status}")
        }
      }

      is Err -> {
        when (val notaryToolError = result.error) {
          is NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError ->
            println("Authentication Error: '${notaryToolError.msg}' Please check your Apple API Key credentials")

          is NotaryToolError.HttpError ->
            println("Http error occurred: ${notaryToolError.responseMetaData.httpStatusCode}, ${notaryToolError.msg}")

          else -> println("Other error: ${notaryToolError.msg}")
        }
      }
    }
  } else {
    log.warn { "Can't get private key file" }
  }
}
