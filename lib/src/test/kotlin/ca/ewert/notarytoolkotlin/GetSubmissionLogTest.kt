package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.prop
import assertk.fail
import ca.ewert.notarytoolkotlin.response.SubmissionId
import ca.ewert.notarytoolkotlin.response.SubmissionLogUrlResponse
import ca.ewert.notarytoolkotlin.response.createMockResponse200
import ca.ewert.notarytoolkotlin.response.createMockResponse401
import ca.ewert.notarytoolkotlin.response.createMockResponse404ErrorResponse
import ca.ewert.notarytoolkotlin.response.createMockResponse404General
import ca.ewert.notarytoolkotlin.response.createMockResponse500
import ca.ewert.notarytoolkotlin.response.createMockResponseConnectionProblem
import ca.ewert.notarytoolkotlin.response.createSubmissionLogResponse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.net.URL
import java.nio.file.Path

private val log = KotlinLogging.logger {}

/**
 * TODO: Add Documentation
 *
 * @author Victor Ewert
 */
class GetSubmissionLogTest : NotaryToolClientTests() {

  /**
   * Tests getSubmissionLog, using valid request to actual Apple Notary API
   */
  @Test
  @Tag("AppleServer")
  @Tag("Private")
  @DisplayName("getSubmissionLog Valid submissionId Test to Apple Server")
  fun getSubmissionLogValidActual() {
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

    val submissionIdResult = SubmissionId.of("4685647e-0125-4343-a068-1c5786499827")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionLogResult = notaryToolClient.getSubmissionLog(submissionId = submissionId)
      assertThat(getSubmissionLogResult).isOk()
      getSubmissionLogResult.onSuccess { submissionLogUrlResponse ->
        assertThat(submissionLogUrlResponse).prop(SubmissionLogUrlResponse::submissionId)
          .prop(SubmissionId::id)
          .isEqualTo("4685647e-0125-4343-a068-1c5786499827")
        assertThat(submissionLogUrlResponse).prop(SubmissionLogUrlResponse::developerLogUrl).isNotNull()
        log.info { "Log URL: ${submissionLogUrlResponse.developerLogUrl}" }
      }
    }
  }

  /**
   * Tests getSubmissionLog, using invalid submissionId to actual Apple Notary API
   * Expect that a [NotaryToolError.UserInputError.InvalidSubmissionIdError] will occur
   */
  @Test
  @Tag("AppleServer")
  @Tag("Private")
  @DisplayName("getSubmissionLog Valid submissionId Test to Apple Server")
  fun getSubmissionLogInvalidActual() {
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

    val submissionIdResult = SubmissionId.of("4685647e-0125-4343-a068-1c5786499828")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionLogResult = notaryToolClient.getSubmissionLog(submissionId = submissionId)
      assertThat(getSubmissionLogResult).isErr()
      getSubmissionLogResult.onFailure { notaryToolError ->
        assertThat(notaryToolError).isInstanceOf<NotaryToolError.UserInputError.InvalidSubmissionIdError>()
        log.info { notaryToolError }
        assertThat(notaryToolError.msg).isEqualTo("There is no resource of type 'submissionsLog' for id '4685647e-0125-4343-a068-1c5786499828'")
      }
    }
  }

  /**
   * Tests getSubmissionInfo, using valid submissionId
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionInfo Valid submissionId Test")
  fun getSubmissionInfoValid() {
    val responseBody: String = """
    {
      "data": {
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog",
        "attributes": {
          "developerLogUrl": "https://notary-artifacts-prod.s3.amazonaws.com/prod/b014d72f-17b6-45ac-abdf-8f39b9241c58/developer_log.json?AWSAccessKeyId=ASIARQRX7CZS5OBQ2DX2&Signature=pk%2FpM0HIEYJ5kBnjKkDERqFsl7g%3D&x-amz-security-token=IQoJb3JpZ2luX2VjELn%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJHMEUCIQCdwq3FSb1cXiPSnqYx38QGnADPl5egcGWycxKTYL6FPAIgYAdj1svOvk2TjJ2MlLnGIthY5cx2%2FW6F74ZJmg%2FtG8cqkQMIMhADGgwxMDQyNzAzMzc2MzciDGLkdSwPEY0GBAdDPiruAj9np%2BsPzfEypASq%2FXeBzimsohiSX59IpIBXAZgIHWqROwVUJlGEtQWjgkOUK%2FeY5Rni8RlO5eeN%2BwZXq%2FFkjEkMo49JKB0A4hnZpB9iN9eFP2z6DokmFhysxmbgwF%2BzHVjpwQFo05Jl9DkhZhiTHdohGHCIyCrV1I%2BRPZd%2B8oCMKZ7xFzsBo%2F4qxnVFeID%2FLXCeOBHbj%2F15O8sLJPOeKDCjo3aT57%2Fy0vz9mxglralTK6eWvyATq9Cz6BWxvUTYFP7umGidN6vG4jLNnHHH0ANamE1d6qoy9dXkVxkAz1ls1cvHCBQ5jMBoOI9Z2cMQv4JziTq20ix9wQljHyy5B9xcHcXNFneA4rGSYVOnhtc8Pieaq4KDmUUJD9t707YwLH6o%2BCDoRzjE9Ia882P%2BJAqM8lJtbkSbofGKkoVJCOBwZ0gOfz3D1ZuVRYZaBYmH%2Bq2lfgYm%2B%2FtQRvsuMSgwKhB7pkvkpWCK4NGMkAb%2FpzCDvZalBjqdAQS%2BQdC0ibh%2FDzz6C2Wxs1%2BI7GnFeRNqVZohEFQRu%2B3HWtPDGXPYmKaS1scnv3tcg3yC9tEZPlkGtxaeuGLF6dQ94A8yfOjX0v8wIUIEj8vGPQ79obTM45GMdmqwJFAcnw8mtjoPrqoJlb9A1741QoAQekFdw0gvnlnz1J7sHkn6DhgTcpCYLLO7M975AMUDWlV%2FAHgtaFtnqD6v9oU%3D&Expires=1688584608"
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

    val getSubmissionLogResult = notaryToolClient.getSubmissionLog(SubmissionId("b014d72f-17b6-45ac-abdf-8f39b9241c58"))
    assertThat(getSubmissionLogResult).isOk()
    getSubmissionLogResult.onSuccess { submissionLogUrlResponse ->
      val expectedUrl = URL(
        "https://notary-artifacts-prod.s3.amazonaws.com/prod/b014d72f-17b6-45ac-abdf-8f39b9241c58/developer_log.json?AWSAccessKeyId=ASIARQRX7CZS5OBQ2DX2&Signature=pk%2FpM0HIEYJ5kBnjKkDERqFsl7g%3D&x-amz-security-token=IQoJb3JpZ2luX2VjELn%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJHMEUCIQCdwq3FSb1cXiPSnqYx38QGnADPl5egcGWycxKTYL6FPAIgYAdj1svOvk2TjJ2MlLnGIthY5cx2%2FW6F74ZJmg%2FtG8cqkQMIMhADGgwxMDQyNzAzMzc2MzciDGLkdSwPEY0GBAdDPiruAj9np%2BsPzfEypASq%2FXeBzimsohiSX59IpIBXAZgIHWqROwVUJlGEtQWjgkOUK%2FeY5Rni8RlO5eeN%2BwZXq%2FFkjEkMo49JKB0A4hnZpB9iN9eFP2z6DokmFhysxmbgwF%2BzHVjpwQFo05Jl9DkhZhiTHdohGHCIyCrV1I%2BRPZd%2B8oCMKZ7xFzsBo%2F4qxnVFeID%2FLXCeOBHbj%2F15O8sLJPOeKDCjo3aT57%2Fy0vz9mxglralTK6eWvyATq9Cz6BWxvUTYFP7umGidN6vG4jLNnHHH0ANamE1d6qoy9dXkVxkAz1ls1cvHCBQ5jMBoOI9Z2cMQv4JziTq20ix9wQljHyy5B9xcHcXNFneA4rGSYVOnhtc8Pieaq4KDmUUJD9t707YwLH6o%2BCDoRzjE9Ia882P%2BJAqM8lJtbkSbofGKkoVJCOBwZ0gOfz3D1ZuVRYZaBYmH%2Bq2lfgYm%2B%2FtQRvsuMSgwKhB7pkvkpWCK4NGMkAb%2FpzCDvZalBjqdAQS%2BQdC0ibh%2FDzz6C2Wxs1%2BI7GnFeRNqVZohEFQRu%2B3HWtPDGXPYmKaS1scnv3tcg3yC9tEZPlkGtxaeuGLF6dQ94A8yfOjX0v8wIUIEj8vGPQ79obTM45GMdmqwJFAcnw8mtjoPrqoJlb9A1741QoAQekFdw0gvnlnz1J7sHkn6DhgTcpCYLLO7M975AMUDWlV%2FAHgtaFtnqD6v9oU%3D&Expires=1688584608",
      )
      assertThat(submissionLogUrlResponse.developerLogUrl).isEqualTo(expectedUrl)
      assertThat(submissionLogUrlResponse.submissionId).isEqualTo(SubmissionId("b014d72f-17b6-45ac-abdf-8f39b9241c58"))
    }
  }

  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionStatus Invalid submissionId Test")
  fun getSubmissionInfoInvalid() {
    val responseBody: String = """
    {
      "errors": [{
        "id": "4b574beb-055a-4fdd-b3e8-a91a58ef7753",
        "status": "404",
        "code": "NOT_FOUND",
        "title": "The specified resource does not exist",
        "detail": "There is no resource of type 'submissionsLog' for id '5685647e-0125-4343-a068-1c5786499827'"
      }]
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse404ErrorResponse(responseBody))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getSubmissionLogResult =
      notaryToolClient.getSubmissionLog(SubmissionId("5685647e-0125-4343-a068-1c5786499827"))
    assertThat(getSubmissionLogResult).isErr()
    getSubmissionLogResult.onFailure { notaryToolError ->
      log.info { notaryToolError }
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.UserInputError.InvalidSubmissionIdError>()
      assertThat(notaryToolError.msg).isEqualTo("There is no resource of type 'submissionsLog' for id '5685647e-0125-4343-a068-1c5786499827'")
    }
  }

  /**
   * Tests getSubmissionLog, with an invalid url, giving a General 404 error.
   * Expect that a [NotaryToolError.HttpError.ClientError4xx] will occur
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionLog 404")
  fun getSubmissionLog404() {
    mockWebServer.enqueue(createMockResponse404General())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getSubmissionLogResult = notaryToolClient.getSubmissionLog(SubmissionId("5685647e-0125-4343-a068-1c5786499827"))
    assertThat(getSubmissionLogResult).isErr()
    getSubmissionLogResult.onFailure { notaryToolError ->
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.HttpError.ClientError4xx>()
      log.info { notaryToolError }
      val httpError: NotaryToolError.HttpError = notaryToolError as NotaryToolError.HttpError.ClientError4xx
      assertThat(httpError.responseMetaData.httpStatusCode).isEqualTo(404)
    }
  }

  /**
   * Tests getSubmissionLog, with an invalid url, giving a 401 Unauthenticated response.
   * Expect that a [NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError] will occur
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionLog 401")
  fun getSubmissionLog401() {
    mockWebServer.enqueue(createMockResponse401())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getSubmissionLogResult = notaryToolClient.getSubmissionLog(SubmissionId("5685647e-0125-4343-a068-1c5786499827"))
    assertThat(getSubmissionLogResult).isErr()
    getSubmissionLogResult.onFailure { notaryToolError ->
      when (notaryToolError) {
        is NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError -> {
          log.info { "Authentication Error" }
          assertThat(notaryToolError.msg).isEqualTo("Notary API Web Service could not authenticate the request.")
        }

        else -> log.warn {
          notaryToolError.msg
          fail(AssertionError("Incorrect Error: $notaryToolError"))
        }
      }
    }
  }

  /**
   * Tests getSubmissionLog, with an invalid url, giving a 403 Unauthenticated response.
   * Expect that a [NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError] will occur
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionLog 403")
  fun getSubmissionLog403() {
    mockWebServer.enqueue(createMockResponse401())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getSubmissionLogResult = notaryToolClient.getSubmissionLog(SubmissionId("5685647e-0125-4343-a068-1c5786499827"))
    assertThat(getSubmissionLogResult).isErr()
    getSubmissionLogResult.onFailure { notaryToolError ->
      when (notaryToolError) {
        is NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError -> {
          log.info { "Authentication Error" }
          assertThat(notaryToolError.msg).isEqualTo("Notary API Web Service could not authenticate the request.")
        }

        else -> log.warn {
          notaryToolError.msg
          fail(AssertionError("Incorrect Error: $notaryToolError"))
        }
      }
    }
  }

  /**
   * Tests getSubmissionLog, with an invalid url, giving a 500 Unauthenticated response.
   * Expect that a [NotaryToolError.HttpError.ServerError5xx] will occur
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionLog 500")
  fun getSubmissionLog500() {
    mockWebServer.enqueue(createMockResponse500())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getSubmissionLogResult = notaryToolClient.getSubmissionLog(SubmissionId("5685647e-0125-4343-a068-1c5786499827"))
    assertThat(getSubmissionLogResult).isErr()
    getSubmissionLogResult.onFailure { notaryToolError ->
      when (notaryToolError) {
        is NotaryToolError.HttpError.ServerError5xx -> {
          log.info { "Authentication Error" }
          assertThat(notaryToolError.responseMetaData.httpStatusCode).isEqualTo(500)
        }

        else -> log.warn {
          notaryToolError.msg
          fail(AssertionError("Incorrect Error: $notaryToolError"))
        }
      }
    }
  }

  /**
   * Tests making a request to getSubmissionLog, simulating a connection issue
   * using [SocketPolicy.NO_RESPONSE]
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionLog No Response Test")
  fun getSubmissionLogNoResponse() {
    mockWebServer.enqueue(createMockResponseConnectionProblem(SocketPolicy.NO_RESPONSE))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult =
      notaryToolClient.getSubmissionLog(SubmissionId("5685647e-0125-4343-a068-1c5786499827"))
    assertThat(getPreviousSubmissionsResult).isErr()
    getPreviousSubmissionsResult.onFailure { notaryToolError ->
      when (notaryToolError) {
        is NotaryToolError.ConnectionError -> {
          log.info { notaryToolError }
          assertThat(notaryToolError.msg).isEqualTo("timeout")
        }

        else -> {
          log.warn { notaryToolError.msg }
          fail(AssertionError("Incorrect Error: $notaryToolError"))
        }
      }
    }
  }

  /**
   * Tests making a request to getSubmissionLog, simulating a connection issue
   * using [SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY]
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionLog Disconnect During Response Test")
  fun getSubmissionLogDisconnect() {
    mockWebServer.enqueue(createMockResponseConnectionProblem(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult =
      notaryToolClient.getSubmissionLog(SubmissionId("5685647e-0125-4343-a068-1c5786499827"))
    assertThat(getPreviousSubmissionsResult).isErr()
    getPreviousSubmissionsResult.onFailure { notaryToolError ->
      when (notaryToolError) {
        is NotaryToolError.ConnectionError -> {
          log.info { notaryToolError }
          assertThat(notaryToolError.msg).isEqualTo("unexpected end of stream")
        }

        else -> {
          log.warn { notaryToolError.msg }
          fail(AssertionError("Incorrect Error: $notaryToolError"))
        }
      }
    }
  }

  /**
   * Tests retrieveSubmissionLog
   */
  @Test
  @Tag("AppleServer")
  @Tag("Private")
  @DisplayName("Retrieve Submission Log Test Actual")
  fun retrieveSubmissionLogActualTest() {
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

    val submissionLogResult =
      notaryToolClient.retrieveSubmissionLog(SubmissionId(("b014d72f-17b6-45ac-abdf-8f39b9241c58")))
    submissionLogResult.onSuccess { logString: String ->
      log.info { "\n$logString" }
      assertThat(logString).isNotEmpty()
    }

    submissionLogResult.onFailure { notaryToolError ->
      fail(AssertionError("Notary Tool Error: $notaryToolError"))
    }
  }

  /**
   * Tests retrieveSubmissionLog
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Retrieve Submission Log Test")
  fun retrieveSubmissionLogTest() {
    mockWebServer.start()

    val baseUrl = mockWebServer.url("")

    val developerLogUrl: HttpUrl = mockWebServer.url(
      "/prod/4685647e-0000-4343-a068-1c5786499827/developer_log.json?AWSAccessKeyId=VEIARQRX7CZSSQ7NXF3S&Signature=q28cXIrrf1%2B4VfAngZB6JRCYGHA%3D&x-amz-security-token=IQoJb7JpZ2luX2VjEL3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJHMEUCIQDFgFbNpWIEmgRJ%2BmEjLDx0ArsR2QKy4E5%2B3XhoTOwGPwIgLDNF5NlsOUbCkJ9ekW2UybnahrBbV4npIDayfSQsjngqkQMINhADGgwxMDQyNzAzMzc2MzciDD%2FeNZidfAXw1BB7hSruAnHu7E2iRW2tNAzMKNlAWvNUOUgowOni7ouvGYWI7EM6TlK8NOCX36TlfG8WsJu8i43bGrtkIT9ahF6q%2FqAeH6cGOFKQyWxqTL37vFs4cFreQ2tRbQHwhhuCLVRBkUkVOxpwaXXRo%2F557zAvH0dMFDYAJ3icsBFyBxTsLhc2CuStVkYszzToMBHTLo8lZZMd8nN0YeeKgEx6rDiZyIg7M31%2FS%2B1W%2B1NRmx%2F1OnnYgELEkLrk5PEojhXGChin%2BwRsUKPvRxl4JaEqJYYdJHoIWYyD%2Fgdno4W7K3qMm1FMXhxeQlYj87o%2FnTzfcyxM5GTgBxiH%2FDjeNJwEm6htBh7iG880nM8b7WuzloYrBRA%2BC3dOayg9Wt6wAvHh9Us%2FVBi%2By4isj95U4ALiBCybcbPqPU8TJGj8aEfUb9RwaeMQ3xYBsthKxKUxrP1Kx7rxNSGqIpRMgiJ4d3itEJA0mgdAIKzHtabMWxNKtT7SslqCLTDDpZelBjqdAZjp58V8I8KmuKm55s5OcOCQYE8DP3rR79fI3qqFVEp3WYGAxF%2F6V5%2Bi90BM1pJvyDcb4PlpsGtrL8Iiugvq2hphN0Wt%2FHUfCzA1ZPKnHdoqIviRcw1J8NmrJVLlKJuzRC9VDlB%2Fo6VQPgP5eW81fxzrapKNHlgtWv%2FXjPa2TcGp35AlQewGtzYoMk5kWAZ%2FKDuTh2DPZRtTftHnUuE%3D&Expires=1688597271",
    )

    val responseBody: String = """
    {
      "data": {
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog",
        "attributes": {
          "developerLogUrl": "$developerLogUrl"
        }
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))

    mockWebServer.enqueue(createSubmissionLogResponse())

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionLogResult =
      notaryToolClient.retrieveSubmissionLog(SubmissionId(("b014d72f-17b6-45ac-abdf-8f39b9241c58")))
    submissionLogResult.onSuccess { logString: String ->
      log.info { "\n$logString" }
      assertThat(logString).isNotEmpty()
    }

    submissionLogResult.onFailure { notaryToolError ->
      fail(AssertionError("Notary Tool Error: $notaryToolError"))
    }
  }

  /**
   * Tests retrieveSubmissionLog with an invalid submission log url
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Retrieve Submission Log Invalid URL Test 1")
  fun retrieveSubmissionLogInvalidUrlTest1() {
    mockWebServer.start()

    val baseUrl = mockWebServer.url("")

    val responseBody: String = """
    {
      "data": {
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog",
        "attributes": {
          "developerLogUrl": "bad/url"
        }
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))
    mockWebServer.enqueue(createSubmissionLogResponse())

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionLogResult =
      notaryToolClient.retrieveSubmissionLog(SubmissionId(("b014d72f-17b6-45ac-abdf-8f39b9241c58")))

    assertThat(submissionLogResult).isErr()
    submissionLogResult.onFailure { notaryToolError ->
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.GeneralError>()
      log.info { "$notaryToolError" }
      assertThat(notaryToolError.msg).isEqualTo("Invalid submission log URL: Expected URL scheme 'http' or 'https' but no scheme was found for bad/ur....")
    }
  }

  /**
   * Tests retrieveSubmissionLog with an invalid submission log url
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Retrieve Submission Log Invalid URL Test 2")
  fun retrieveSubmissionLogInvalidUrlTest2() {
    mockWebServer.start()

    val baseUrl = mockWebServer.url("")

    val responseBody: String = """
    {
      "data": {
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog",
        "attributes": {
          "developerLogUrl": "http://\//\/\/"
        }
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))
    mockWebServer.enqueue(createSubmissionLogResponse())

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionLogResult =
      notaryToolClient.retrieveSubmissionLog(SubmissionId(("b014d72f-17b6-45ac-abdf-8f39b9241c58")))

    assertThat(submissionLogResult).isErr()
    submissionLogResult.onFailure { notaryToolError ->
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.GeneralError>()
      log.info { "$notaryToolError" }
      assertThat(notaryToolError.msg).isEqualTo("Invalid submission log URL: Invalid URL host: \"\".")
    }
  }

  /**
   * Tests retrieveSubmissionLog with a blank submission log url
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Retrieve Submission Log Blank URL Test")
  fun retrieveSubmissionLogBlankUrlTest() {
    mockWebServer.start()

    val baseUrl = mockWebServer.url("")

    val responseBody: String = """
    {
      "data": {
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog",
        "attributes": {
          "developerLogUrl": ""
        }
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))
    mockWebServer.enqueue(createSubmissionLogResponse())

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionLogResult =
      notaryToolClient.retrieveSubmissionLog(SubmissionId(("b014d72f-17b6-45ac-abdf-8f39b9241c58")))

    assertThat(submissionLogResult).isErr()
    submissionLogResult.onFailure { notaryToolError ->
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.GeneralError>()
      log.info { "$notaryToolError" }
      assertThat(notaryToolError.msg).isEqualTo("Invalid submission log URL: Expected URL scheme 'http' or 'https' but no scheme was found for .")
    }
  }

  /**
   * Tests retrieveSubmissionLog with 404 response
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Retrieve Submission Log 404 Test")
  fun retrieveSubmissionLog404Test() {
    mockWebServer.start()

    val baseUrl = mockWebServer.url("")
    val developerLogUrl: HttpUrl = mockWebServer.url(
      "/prod/4685647e-0000-4343-a068-1c5786499827/developer_log.json?AWSAccessKeyId=VEIARQRX7CZSSQ7NXF3S&Signature=q28cXIrrf1%2B4VfAngZB6JRCYGHA%3D&x-amz-security-token=IQoJb7JpZ2luX2VjEL3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJHMEUCIQDFgFbNpWIEmgRJ%2BmEjLDx0ArsR2QKy4E5%2B3XhoTOwGPwIgLDNF5NlsOUbCkJ9ekW2UybnahrBbV4npIDayfSQsjngqkQMINhADGgwxMDQyNzAzMzc2MzciDD%2FeNZidfAXw1BB7hSruAnHu7E2iRW2tNAzMKNlAWvNUOUgowOni7ouvGYWI7EM6TlK8NOCX36TlfG8WsJu8i43bGrtkIT9ahF6q%2FqAeH6cGOFKQyWxqTL37vFs4cFreQ2tRbQHwhhuCLVRBkUkVOxpwaXXRo%2F557zAvH0dMFDYAJ3icsBFyBxTsLhc2CuStVkYszzToMBHTLo8lZZMd8nN0YeeKgEx6rDiZyIg7M31%2FS%2B1W%2B1NRmx%2F1OnnYgELEkLrk5PEojhXGChin%2BwRsUKPvRxl4JaEqJYYdJHoIWYyD%2Fgdno4W7K3qMm1FMXhxeQlYj87o%2FnTzfcyxM5GTgBxiH%2FDjeNJwEm6htBh7iG880nM8b7WuzloYrBRA%2BC3dOayg9Wt6wAvHh9Us%2FVBi%2By4isj95U4ALiBCybcbPqPU8TJGj8aEfUb9RwaeMQ3xYBsthKxKUxrP1Kx7rxNSGqIpRMgiJ4d3itEJA0mgdAIKzHtabMWxNKtT7SslqCLTDDpZelBjqdAZjp58V8I8KmuKm55s5OcOCQYE8DP3rR79fI3qqFVEp3WYGAxF%2F6V5%2Bi90BM1pJvyDcb4PlpsGtrL8Iiugvq2hphN0Wt%2FHUfCzA1ZPKnHdoqIviRcw1J8NmrJVLlKJuzRC9VDlB%2Fo6VQPgP5eW81fxzrapKNHlgtWv%2FXjPa2TcGp35AlQewGtzYoMk5kWAZ%2FKDuTh2DPZRtTftHnUuE%3D&Expires=1688597271",
    )

    val responseBody: String = """
    {
      "data": {
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog",
        "attributes": {
          "developerLogUrl": "$developerLogUrl"
        }
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))
    mockWebServer.enqueue(createMockResponse404General())

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionLogResult =
      notaryToolClient.retrieveSubmissionLog(SubmissionId(("b014d72f-17b6-45ac-abdf-8f39b9241c58")))

    assertThat(submissionLogResult).isErr()
    submissionLogResult.onFailure { notaryToolError ->
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.HttpError.ClientError4xx>()
      if (notaryToolError is NotaryToolError.HttpError.ClientError4xx) {
        assertThat(notaryToolError.responseMetaData.httpStatusCode).isEqualTo(404)
        log.info { notaryToolError.responseMetaData.httpStatusMessage }
      }
    }
  }

  /**
   * Tests retrieveSubmissionLog with 500 response
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Retrieve Submission Log 500 Test")
  fun retrieveSubmissionLog500Test() {
    mockWebServer.start()

    val baseUrl = mockWebServer.url("")

    val developerLogUrl: HttpUrl = mockWebServer.url(
      "/prod/4685647e-0000-4343-a068-1c5786499827/developer_log.json?AWSAccessKeyId=VEIARQRX7CZSSQ7NXF3S&Signature=q28cXIrrf1%2B4VfAngZB6JRCYGHA%3D&x-amz-security-token=IQoJb7JpZ2luX2VjEL3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJHMEUCIQDFgFbNpWIEmgRJ%2BmEjLDx0ArsR2QKy4E5%2B3XhoTOwGPwIgLDNF5NlsOUbCkJ9ekW2UybnahrBbV4npIDayfSQsjngqkQMINhADGgwxMDQyNzAzMzc2MzciDD%2FeNZidfAXw1BB7hSruAnHu7E2iRW2tNAzMKNlAWvNUOUgowOni7ouvGYWI7EM6TlK8NOCX36TlfG8WsJu8i43bGrtkIT9ahF6q%2FqAeH6cGOFKQyWxqTL37vFs4cFreQ2tRbQHwhhuCLVRBkUkVOxpwaXXRo%2F557zAvH0dMFDYAJ3icsBFyBxTsLhc2CuStVkYszzToMBHTLo8lZZMd8nN0YeeKgEx6rDiZyIg7M31%2FS%2B1W%2B1NRmx%2F1OnnYgELEkLrk5PEojhXGChin%2BwRsUKPvRxl4JaEqJYYdJHoIWYyD%2Fgdno4W7K3qMm1FMXhxeQlYj87o%2FnTzfcyxM5GTgBxiH%2FDjeNJwEm6htBh7iG880nM8b7WuzloYrBRA%2BC3dOayg9Wt6wAvHh9Us%2FVBi%2By4isj95U4ALiBCybcbPqPU8TJGj8aEfUb9RwaeMQ3xYBsthKxKUxrP1Kx7rxNSGqIpRMgiJ4d3itEJA0mgdAIKzHtabMWxNKtT7SslqCLTDDpZelBjqdAZjp58V8I8KmuKm55s5OcOCQYE8DP3rR79fI3qqFVEp3WYGAxF%2F6V5%2Bi90BM1pJvyDcb4PlpsGtrL8Iiugvq2hphN0Wt%2FHUfCzA1ZPKnHdoqIviRcw1J8NmrJVLlKJuzRC9VDlB%2Fo6VQPgP5eW81fxzrapKNHlgtWv%2FXjPa2TcGp35AlQewGtzYoMk5kWAZ%2FKDuTh2DPZRtTftHnUuE%3D&Expires=1688597271",
    )

    val responseBody: String = """
    {
      "data": {
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog",
        "attributes": {
          "developerLogUrl": "$developerLogUrl"
        }
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))
    mockWebServer.enqueue(createMockResponse500())

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionLogResult =
      notaryToolClient.retrieveSubmissionLog(SubmissionId(("b014d72f-17b6-45ac-abdf-8f39b9241c58")))

    assertThat(submissionLogResult).isErr()
    submissionLogResult.onFailure { notaryToolError ->
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.HttpError.ServerError5xx>()
      if (notaryToolError is NotaryToolError.HttpError.ServerError5xx) {
        assertThat(notaryToolError.responseMetaData.httpStatusCode).isEqualTo(500)
        log.info { notaryToolError.responseMetaData.httpStatusMessage }
      }
    }
  }
}
