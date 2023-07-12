package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNotZero
import ca.ewert.notarytoolkotlin.response.createMockResponse200
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

private val log = KotlinLogging.logger {}

/**
 * Unit tests for [NotaryToolClient.submitSoftware]
 *
 * @author Victor Ewert
 */
class SubmitSoftwareTests : NotaryToolClientTests() {

  /**
   * Test the function that calculates the SHA-256 hash of a file.
   */
  @Test
  @DisplayName("SHA-256 - Test")
  fun sha256Test() {
    val testFile1: Path? = resourceToPath("/PWMinder.zip")
    assertThat(testFile1).isNotNull()
    if (testFile1 != null) {
      val result = calculateSha256(testFile1)
      assertThat(result).isEqualTo("422f7c6b57492b99ec0803599e7e44941477b0a5c3253f5dc88eb90c672fed08")
    }

    val testFile2: Path? = resourceToPath("/pwm_3.3.3.0_aarch64.dmg")
    assertThat(testFile2).isNotNull()
    if (testFile2 != null) {
      val result = calculateSha256(testFile2)
      assertThat(result).isEqualTo("35775b0a826e9f5251cbef711648b506173f3ece5519e8e0e0fdd7213b3396c3")
    }
  }

  /**
   * Verifies that the request is created correctly.
   * Verifies the method is POST, and `Content-Type = application/json; charset=utf-8`
   * and that the request body json is correct.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Check Request - Test")
  fun checkRequestTest() {
    mockWebServer.enqueue(createMockResponse200(""))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")
    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val softwareFile: Path? = resourceToPath("/pwm_3.3.3.0_aarch64.dmg")
    assertThat(softwareFile).isNotNull()
    if (softwareFile != null) {
      notaryToolClient.submitSoftware(softwareFile)
      val expectedBody = """
      {"notifications":[],"sha256":"35775b0a826e9f5251cbef711648b506173f3ece5519e8e0e0fdd7213b3396c3","submissionName":"pwm_3.3.3.0_aarch64.dmg"}
      """.trimIndent()
      val request = mockWebServer.takeRequest()
      assertThat(request.method).isEqualTo("POST")
      assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=utf-8")
      assertThat(request.body.readString(StandardCharsets.UTF_8)).isEqualTo(expectedBody)
    }
  }

  @Test
  @Tag("AppleServer")
  @Tag("Private")
  @DisplayName("Submit Software Success Apple - Test")
  fun submitSoftwareSuccessActual() {
    val testValuesReader = TestValuesReader()
    val keyId: String = testValuesReader.getKeyId()
    val issuerId: String = testValuesReader.getIssueId()
    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

    assertThat(privateKeyFile).isNotNull()

    val notaryToolClient = NotaryToolClient(
      privateKeyId = keyId,
      issuerId = issuerId,
      privateKeyFile = privateKeyFile!!,
    )
    val softwareFile: Path? = resourceToPath("/pwm_3.3.3.0_aarch64.dmg")
    assertThat(softwareFile).isNotNull()

    val newSubmissionResponseResult = notaryToolClient.submitSoftware(softwareFile!!)
    assertThat(newSubmissionResponseResult).isOk()

    newSubmissionResponseResult.onSuccess { newSubmissionResponse ->
      log.info { newSubmissionResponse }
      assertThat(newSubmissionResponse.responseMetaData.rawContents!!.length).isNotZero()
    }
  }

  /**
   * Tests sending submit software request with a successful result.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Submit Software Valid - Test")
  fun submitSoftwareValid() {
    val responseBody = """
    {
      "data": {
        "type": "newSubmissions",
        "id": "1238aa04-1593-4391-96c8-ca1920bc7c7f",
        "attributes": {
          "awsAccessKeyId": "ASIARQRX7CZSYJVTJPBJ",
          "awsSecretAccessKey": "Yckio/Sk+Xjt2skzIUXC55igDapXrjqEEyhUf1Gb",
          "awsSessionToken": "FwoGZXIvYXdzEGEaDFdhVOScqpBYw2R/ASLJARXoN1wbt5ntnmz6PBipCRdOCgH/FZnt35thisPNcRz+TqAMvwXJ74DItF1ozWI+Xkea7nt9qqvC7NbH0ujZqTPeluY+WUZn0HJ7Ebsc8Sm2IjErjGUzk43tBp0Pumn+l1BuRP54Elp3wJ1szo0UFqsO/ovTJII7Sm+7K5s86qSPa+P/hulzHIhOgKvO/YEN4r+FrymcHwVaphbjvpwOQ/BnG5DGPCGuUML9DCUyqshDkkmekpKCibYjigvzGJJadzCGYU5EBpRbZSig1relBjItxqCaD4Fs26gHAZe8T82a6+9Rqm/QGpz3x42lwOl2VB8AYfX7rjJcRVJDityY",
          "bucket": "notary-submissions-prod",
          "object": "prod/AROARQRX7CZS3PRF6ZA5L:1238aa04-1593-4391-96c8-ca1920bc7c7f"
        }
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val softwareFile: Path? = resourceToPath("/pwm_3.3.3.0_aarch64.dmg")
    assertThat(softwareFile).isNotNull()

    val newSubmissionResponseResult = notaryToolClient.submitSoftware(softwareFile!!)

    assertThat(newSubmissionResponseResult).isOk()

    newSubmissionResponseResult.onSuccess { newSubmissionResponse ->
      assertThat(newSubmissionResponse.id.id).isEqualTo("1238aa04-1593-4391-96c8-ca1920bc7c7f")
      assertThat(newSubmissionResponse.awsAccessKeyId).isEqualTo("ASIARQRX7CZSYJVTJPBJ")
      assertThat(newSubmissionResponse.awsSecretAccessKey).isEqualTo("Yckio/Sk+Xjt2skzIUXC55igDapXrjqEEyhUf1Gb")
      assertThat(newSubmissionResponse.awsSessionToken).isEqualTo("FwoGZXIvYXdzEGEaDFdhVOScqpBYw2R/ASLJARXoN1wbt5ntnmz6PBipCRdOCgH/FZnt35thisPNcRz+TqAMvwXJ74DItF1ozWI+Xkea7nt9qqvC7NbH0ujZqTPeluY+WUZn0HJ7Ebsc8Sm2IjErjGUzk43tBp0Pumn+l1BuRP54Elp3wJ1szo0UFqsO/ovTJII7Sm+7K5s86qSPa+P/hulzHIhOgKvO/YEN4r+FrymcHwVaphbjvpwOQ/BnG5DGPCGuUML9DCUyqshDkkmekpKCibYjigvzGJJadzCGYU5EBpRbZSig1relBjItxqCaD4Fs26gHAZe8T82a6+9Rqm/QGpz3x42lwOl2VB8AYfX7rjJcRVJDityY")
      assertThat(newSubmissionResponse.bucket).isEqualTo("notary-submissions-prod")
      assertThat(newSubmissionResponse.objectKey).isEqualTo("prod/AROARQRX7CZS3PRF6ZA5L:1238aa04-1593-4391-96c8-ca1920bc7c7f")
    }
  }

//  /**
//   * Tests submitting an uploading software for notarization.
//   */
//  @Test
//  @Tag("AppleServer")
//  @Tag("Private")
//  @DisplayName("Submit and Upload Actual - Test")
//  suspend fun submitAndUploadActual() {
//    val testValuesReader = TestValuesReader()
//    val keyId: String = testValuesReader.getKeyId()
//    val issuerId: String = testValuesReader.getIssueId()
//    val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")
//
//    assertThat(privateKeyFile).isNotNull()
//
//    val notaryToolClient = NotaryToolClient(
//      privateKeyId = keyId,
//      issuerId = issuerId,
//      privateKeyFile = privateKeyFile!!,
//    )
//    val softwareFile: Path? = resourceToPath("/pwm_3.3.3.0_aarch64.dmg")
//    assertThat(softwareFile).isNotNull()
//
//    val submitResult = notaryToolClient.submitAndUploadSoftware(softwareFile!!)
//    assertThat(submitResult).isOk()
//
//    submitResult.onSuccess { submissionId ->
//      log.info { "Submitted new software. Submission Id: $submissionId" }
//      assertThat(submissionId.id.length).isGreaterThan(0)
//    }
//  }
}
