package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.matches
import assertk.assertions.prop
import assertk.fail
import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import ca.ewert.notarytoolkotlin.response.Status
import ca.ewert.notarytoolkotlin.response.createMockResponse200
import ca.ewert.notarytoolkotlin.response.createMockResponse401
import ca.ewert.notarytoolkotlin.response.createMockResponse403
import ca.ewert.notarytoolkotlin.response.createMockResponse404General
import ca.ewert.notarytoolkotlin.response.createMockResponse500
import ca.ewert.notarytoolkotlin.response.createMockResponseConnectionProblem
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [NotaryToolClient.getPreviousSubmissions]
 *
 * @author Victor Ewert
 */
class GetPreviousSubmissionsTests : NotaryToolClientTests() {
  /**
   * Tests making a request to getPreviousSubmission, with a valid response.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions - Success Test")
  fun getPreviousSubmissionsValid() {
    val responseBody: String = """
    {
      "data": [
        {
          "attributes": {
            "createdDate": "2021-04-29T01:38:09.498Z",
            "name": "OvernightTextEditor_11.6.8.zip",
            "status": "Accepted"
          },
          "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
          "type": "submissions"
        },
        {
          "attributes": {
            "createdDate": "2021-04-23T17:44:54.761Z",
            "name": "OvernightTextEditor_11.6.7.zip",
            "status": "Accepted"
          },
          "id": "cf0c235a-dad2-4c24-96eb-c876d4cb3a2d",
          "type": "submissions"
        },
        {
          "attributes": {
            "createdDate": "2021-04-19T16:56:17.839Z",
            "name": "OvernightTextEditor_11.6.7.zip",
            "status": "Invalid"
          },
          "id": "38ce81cc-0bf7-454b-91ef-3f7395bf297b",
          "type": "submissions"
        }
      ],
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()
    getPreviousSubmissionsResult.onSuccess { submissionListResponse ->
      assertThat(submissionListResponse.submissionInfoList).hasSize(3)
      assertThat(submissionListResponse.submissionInfoList[2].status).isEqualTo(Status.INVALID)
      val expectedCreatedDate: Instant =
        ZonedDateTime.of(2021, 4, 23, 17, 44, 54, 761000000, ZoneId.of("GMT")).toInstant()
      assertThat(submissionListResponse.submissionInfoList[1].createdDate ?: Instant.now())
        .isEqualTo(expectedCreatedDate)

      val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
      assertThat(recordedRequest.getHeader("User-Agent") ?: "").matches(Regex("notarytool-kotlin/\\d+\\.\\d+\\.\\d+"))
      assertThat(recordedRequest.requestUrl?.toString() ?: "").endsWith("/submissions")
    }

    getPreviousSubmissionsResult.onFailure { error ->
      when (error) {
        is NotaryToolError.UserInputError.JsonWebTokenError.TokenCreationError -> log.warn { error.msg }
        is NotaryToolError.HttpError -> log.warn { "An HTTP Error occurred. Code: ${error.httpStatusCode} - ${error.httpStatusMsg}, for request to: ${error.requestUrl}" }
        else -> log.warn { error.msg }
      }
      fail(AssertionError("Request failed with: $error"))
    }
  }

  /**
   * Tests making a request to getPreviousSubmission, with a 401 - Unauthenticated
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions - 401 Test")
  fun getPreviousSubmissions401() {
    mockWebServer.enqueue(createMockResponse401())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()

    assertThat(getPreviousSubmissionsResult).isErr()

    getPreviousSubmissionsResult.onFailure { error ->
      log.info { "onFailure(): $error" }
      when (error) {
        is NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError -> {
          log.info { "Authentication Error" }
          assertThat(error.msg).isEqualTo("Notary API Web Service could not authenticate the request.")
        }

        else -> log.warn {
          error.msg
          fail(AssertionError("Incorrect Error: $error"))
        }
      }
    }
  }

  /**
   * Tests making a request to getPreviousSubmissions, with a 403 - Unauthenticated
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions - 403 Test")
  fun getPreviousSubmissions403() {
    mockWebServer.enqueue(createMockResponse403())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()

    assertThat(getPreviousSubmissionsResult).isErr()

    getPreviousSubmissionsResult.onFailure { error ->
      log.info { "onFailure(): $error" }
      when (error) {
        is NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError -> {
          log.info { "Authentication Error" }
          assertThat(error.msg).isEqualTo("Notary API Web Service could not authenticate the request.")
        }

        else -> log.warn {
          error.msg
          fail(AssertionError("Incorrect Error: $error"))
        }
      }
    }
  }

  /**
   * Tests getPreviousSubmissions, with an invalid url, giving a 404 error.
   * Expect that a [NotaryToolError.HttpError.ClientError4xx] will occur
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions 404")
  fun getPreviousSubmissions404() {
    mockWebServer.enqueue(createMockResponse404General())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()
    assertThat(getPreviousSubmissionsResult).isErr()
    getPreviousSubmissionsResult.onFailure { notaryToolError ->
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.HttpError.ClientError4xx>()
      log.info() { notaryToolError }
      val httpError: NotaryToolError.HttpError = notaryToolError as NotaryToolError.HttpError.ClientError4xx
      assertThat(httpError).prop(NotaryToolError.HttpError::httpStatusCode).isEqualTo(404)
    }
  }

  /**
   * Tests getPreviousSubmissions, with a response code of 500.
   * Expect that a [NotaryToolError.HttpError.ClientError4xx] will occur
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions 500")
  fun getPreviousSubmissions500() {
    mockWebServer.enqueue(createMockResponse500())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()
    assertThat(getPreviousSubmissionsResult).isErr()
    getPreviousSubmissionsResult.onFailure { notaryToolError ->
      assertThat(notaryToolError).isInstanceOf<NotaryToolError.HttpError.ServerError5xx>()
      log.info() { notaryToolError }
      val httpError: NotaryToolError.HttpError = notaryToolError as NotaryToolError.HttpError.ServerError5xx
      assertThat(httpError).prop(NotaryToolError.HttpError::httpStatusCode).isEqualTo(500)
      assertThat(httpError).prop(NotaryToolError::msg)
        .isEqualTo("The request was unsuccessful, a Server error occurred.")
    }
  }

  /**
   * Tests making a request to getPreviousSubmission, with unexpected json in the response body.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions - Invalid json")
  fun getPreviousSubmissionsInvalidJson() {
    val responseBody: String = """
    {
      "datas": [
        {
          "attributes": {
            "createdDate": "2021-04-29T01:38:09.498Z",
            "name": "OvernightTextEditor_11.6.8.zip",
            "status": "Accepted"
          },
          "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
          "type": "submissions"
        },
        {
          "attributes": {
            "createdDate": "2021-04-23T17:44:54.761Z",
            "name": "OvernightTextEditor_11.6.7.zip",
            "status": "Accepted"
          },
          "id": "cf0c235a-dad2-4c24-96eb-c876d4cb3a2d",
          "type": "submissions"
        },
        {
          "attributes": {
            "createdDate": "2021-04-19T16:56:17.839Z",
            "name": "OvernightTextEditor_11.6.7.zip",
            "status": "Invalid"
          },
          "id": "38ce81cc-0bf7-454b-91ef-3f7395bf297b",
          "type": "submissions"
        }
      ],
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()
    assertThat(getPreviousSubmissionsResult).isErr()

    getPreviousSubmissionsResult.onFailure { error ->
      when (error) {
        is NotaryToolError.JsonParseError -> {
          log.info { error }
          assertThat(error.msg).isEqualTo("Error parsing json: Cannot skip unexpected NAME at \$.datas.")
          assertThat(error.jsonString).isEqualTo(responseBody)
        }

        else -> {
          log.warn { error.msg }
          fail(AssertionError("Incorrect Error: $error"))
        }
      }
    }
  }

  /**
   * Tests making a request to getPreviousSubmission, with a valid response, but no
   * submission data.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions - No data")
  fun getPreviousSubmissionsNoData() {
    val responseBody: String = """
    {
      "data": [
      ],
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBody))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()
    assertThat(getPreviousSubmissionsResult).isOk()

    getPreviousSubmissionsResult.onSuccess { submissionListResponse ->
      assertThat(submissionListResponse.submissionInfoList).hasSize(0)
    }

    getPreviousSubmissionsResult.onFailure { error ->
      fail(AssertionError("Request failed with: $error"))
    }
  }

  /**
   * Tests making a request to getSubmissionStatus, simulating a connection issue
   * using [SocketPolicy.NO_RESPONSE]
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions No Response Test")
  fun getPreviousSubmissionsNoResponse() {
    mockWebServer.enqueue(createMockResponseConnectionProblem(SocketPolicy.NO_RESPONSE))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()
    assertThat(getPreviousSubmissionsResult).isErr()
    getPreviousSubmissionsResult.onFailure { notaryToolError ->
      when (notaryToolError) {
        is NotaryToolError.ConnectionError -> {
          log.info { notaryToolError }
        }

        else -> {
          log.warn { notaryToolError.msg }
          fail(AssertionError("Incorrect Error: $notaryToolError"))
        }
      }
    }
  }

  /**
   * Tests making a request to getSubmissionStatus, simulating a connection issue
   * using [SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY]
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions Disconnect During Response Test")
  fun getPreviousSubmissionsDisconnect() {
    mockWebServer.enqueue(createMockResponseConnectionProblem(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()
    assertThat(getPreviousSubmissionsResult).isErr()
    getPreviousSubmissionsResult.onFailure { notaryToolError ->
      when (notaryToolError) {
        is NotaryToolError.ConnectionError -> {
          log.info { notaryToolError }
        }

        else -> {
          log.warn { notaryToolError.msg }
          fail(AssertionError("Incorrect Error: $notaryToolError"))
        }
      }
    }
  }
}
