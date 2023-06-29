package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.matches
import assertk.fail
import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import ca.ewert.notarytoolkotlin.errors.NotaryToolError.UserInputError.JsonWebTokenError
import ca.ewert.notarytoolkotlin.http.response.SubmissionId
import ca.ewert.notarytoolkotlin.http.response.SubmissionStatus
import ca.ewert.notarytoolkotlin.http.response.createMockResponse200
import ca.ewert.notarytoolkotlin.http.response.createMockResponse401
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.Protocol
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val log = KotlinLogging.logger {}

/**
 * Unit Tests for the [NotaryToolClient]
 */
class NotaryToolClientTests {

  private val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

  private var mockWebServer: MockWebServer

  init {
    mockWebServer = MockWebServer()
  }

  @BeforeEach
  fun setup() {
    mockWebServer = MockWebServer()
    mockWebServer.protocols = listOf(Protocol.HTTP_1_1, Protocol.HTTP_2)
  }

  @AfterEach
  fun tearDown() {
    mockWebServer.shutdown()
  }

  /**
   * Tests making a request to getPreviousSubmission, with a valid response.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getPreviousSubmissions - Success Test")
  fun getPreviousSubmissionsTest1() {
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
        is JsonWebTokenError.TokenCreationError -> log.warn { error.msg }
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
  fun getPreviousSubmissionTest2() {
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
        is JsonWebTokenError.AuthenticationError -> {
          log.info { "Authentication Error" }
          assertThat(error.msg).isEqualTo("Web Service could not authenticate the request.")
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
  fun getPreviousSubmissionsTest3() {
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
  fun getPreviousSubmissionsTest4() {
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
   * Tests getSubmissionStatus, using valid request to actual Apple Notary API
   */
  @Test
  @Tag("Apple-Server")
  @Tag("Private")
  @DisplayName("getSubmissionStatus Valid submissionId Test to Apple Server")
  fun getSubmissionStatusValidActual() {
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
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isOk()
      getSubmissionStatusResult.onSuccess { submissionStatusResponse ->
        assertThat(submissionStatusResponse.submissionInfo.name).isEqualTo("PWMinder.zip")
      }
    }
  }

  /**
   * Tests getSubmissionStatus, using invalid submissionId to actual Apple Notary API
   * Expect that a [NotaryToolError.UserInputError.InvalidSubmissionIdError] will occur
   */
  @Test
  @Tag("Apple-Server")
  @Tag("Private")
  @DisplayName("getSubmissionStatus Valid submissionId Test to Apple Server")
  fun getSubmissionStatusInvalidActual() {
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
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isErr()
      getSubmissionStatusResult.onFailure { notaryToolError ->
        assertThat(notaryToolError).isInstanceOf<NotaryToolError.UserInputError.InvalidSubmissionIdError>()
        log.info() { notaryToolError }
        assertThat(notaryToolError.msg).isEqualTo("There is no resource of type 'submissions' with id '4685647e-0125-4343-a068-1c5786499828'")
      }
    }
  }
}
