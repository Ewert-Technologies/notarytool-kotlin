package ca.ewert.notarytoolkotlin.examples.kotlin

import ca.ewert.notarytoolkotlin.NotaryToolClient
import ca.ewert.notarytoolkotlin.NotaryToolError
import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError
import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError.PrivateKeyNotFoundError
import ca.ewert.notarytoolkotlin.TestValuesReader
import ca.ewert.notarytoolkotlin.resourceToPath
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.useLines

private val log = KotlinLogging.logger {}

/**
 * Example of getting the Submission History
 */
fun main() {
  exampleUsingPrivateKeyPath()
  exampleUsingPrivateKeyProvider()
}

/**
 * This example uses the private key file Path.
 */
private fun exampleUsingPrivateKeyPath() {
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

    println("Token Lifetime: ${notaryToolClient.tokenLifetime}")
    println("Connect Timeout: ${notaryToolClient.connectTimeout}")
    println(notaryToolClient.userAgent)

    val result = notaryToolClient.getPreviousSubmissions()
    when {
      result.isOk -> {
        val submissionListResponse = result.value
        println(
          "Response Received on: ${
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.LONG)
              .format(submissionListResponse.receivedTimestamp.atZone(ZoneId.systemDefault()))
          }",
        )
        println("Response Status: ${submissionListResponse.responseMetaData.httpStatusString}")
        submissionListResponse.submissionInfoList.forEach { submissionInfo ->
          println("${submissionInfo.createdDate}\t${submissionInfo.id}\t${submissionInfo.status}\t${submissionInfo.name}")
        }
      }

      else -> {
        when (val notaryToolError = result.error) {
          is NotaryToolError.UserInputError.AuthenticationError ->
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

/**
 * This example uses the private key provider.
 */
private fun exampleUsingPrivateKeyProvider() {
  val testValuesReader = TestValuesReader()
  val keyId: String = testValuesReader.getKeyId()
  val issuerId: String = testValuesReader.getIssueId()
  val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

  val notaryToolClient = NotaryToolClient(
    privateKeyId = keyId,
    issuerId = issuerId,
    privateKeyProvider = ::privateKeyProvider, // privateKeyProvider is a function that creates an ECPrivateKey
  )

  println("Token Lifetime: ${notaryToolClient.tokenLifetime}")
  println("Connect Timeout: ${notaryToolClient.connectTimeout}")
  println(notaryToolClient.userAgent)

  val result = notaryToolClient.getPreviousSubmissions()
  when {
    result.isOk -> {
      val submissionListResponse = result.value
      println(
        "Response Received on: ${
          DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.LONG)
            .format(submissionListResponse.receivedTimestamp.atZone(ZoneId.systemDefault()))
        }",
      )
      println("Response Status: ${submissionListResponse.responseMetaData.httpStatusString}")
      submissionListResponse.submissionInfoList.forEach { submissionInfo ->
        println("${submissionInfo.createdDate}\t${submissionInfo.id}\t${submissionInfo.status}\t${submissionInfo.name}")
      }
    }

    else -> {
      when (val notaryToolError = result.error) {
        is NotaryToolError.UserInputError.AuthenticationError ->
          println("Authentication Error: '${notaryToolError.msg}' Please check your Apple API Key credentials")

        is NotaryToolError.HttpError ->
          println("Http error occurred: ${notaryToolError.responseMetaData.httpStatusCode}, ${notaryToolError.msg}")

        else -> println("Other error: ${notaryToolError.msg}")
      }
    }
  }
}

/**
 * Creates an ECPrivateKey using a private key file.
 *
 * @return an ECPrivateKey or a JsonWebTokenError
 */
internal fun privateKeyProvider(): Result<ECPrivateKey, JsonWebTokenError> {
  val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")
  return if (privateKeyFile!!.exists()) {
    val keyBytes: ByteArray = privateKeyFile.useLines { lines ->
      lines.filter { !it.matches(Regex("-*\\w+ PRIVATE KEY-*")) }.joinToString(separator = "").toByteArray()
    }
    try {
      val keyBytesBase64Decoded: ByteArray = Base64.getDecoder().decode(keyBytes)
      val pkcS8EncodedKeySpec = PKCS8EncodedKeySpec(keyBytesBase64Decoded)
      Ok(KeyFactory.getInstance("EC").generatePrivate(pkcS8EncodedKeySpec) as ECPrivateKey)
    } catch (exception: Exception) {
      Err(
        JsonWebTokenError
          .InvalidPrivateKeyError(exceptionMsg = exception.message ?: "N/A"),
      )
    }
  } else {
    Err(PrivateKeyNotFoundError(privateKeyFile))
  }
}
