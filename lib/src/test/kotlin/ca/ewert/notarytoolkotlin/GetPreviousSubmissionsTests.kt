package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.matches
import assertk.fail
import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import ca.ewert.notarytoolkotlin.http.response.SubmissionStatus
import ca.ewert.notarytoolkotlin.http.response.createMockResponse200
import ca.ewert.notarytoolkotlin.http.response.createMockResponse401
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.mockwebserver.RecordedRequest
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
      assertThat(submissionListResponse.submissionInfoList[2].status).isEqualTo(SubmissionStatus.INVALID)
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
}
