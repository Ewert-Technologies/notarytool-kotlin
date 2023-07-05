package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
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
  @Tag("Apple-Server")
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
  @Tag("Apple-Server")
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
        log.info() { notaryToolError }
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
      log.info() { notaryToolError }
      val httpError: NotaryToolError.HttpError = notaryToolError as NotaryToolError.HttpError.ClientError4xx
      assertThat(httpError).prop(NotaryToolError.HttpError::httpStatusCode).isEqualTo(404)
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
          assertThat(notaryToolError.httpStatusCode).isEqualTo(500)
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
}
