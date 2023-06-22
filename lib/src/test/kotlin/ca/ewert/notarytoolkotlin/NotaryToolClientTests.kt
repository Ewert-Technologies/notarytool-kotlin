package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.*
import assertk.fail
import ca.ewert.notarytoolkotlin.errors.JsonWebTokenError
import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import ca.ewert.notarytoolkotlin.http.response.SubmissionStatus
import ca.ewert.notarytoolkotlin.http.response.createMockResponse
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.Protocol
import okhttp3.internal.userAgent
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val log = KotlinLogging.logger {}

/**
 * TODO: Add Comments
 * <br/>
 * Created: 2023-06-19
 * @author vewert
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

  @Test
  fun test1() {
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
    }""".trimIndent()

    mockWebServer.enqueue(createMockResponse(responseBody))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")


    val notaryToolClient = NotaryToolClient(
      privateKeyId = TestValuesReader().getKeyId(),
      issuerId = TestValuesReader().getIssueId(),
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString()
    )

    val getPreviousSubmissionsResult = notaryToolClient.getPreviousSubmissions()
    getPreviousSubmissionsResult.onSuccess { submissionListResponse ->
      assertThat(submissionListResponse.submissionInfoList).hasSize(3)
      assertThat(submissionListResponse.submissionInfoList[2].status).isEqualTo(SubmissionStatus.INVALID)
      val expectedCreatedDate: Instant =
        ZonedDateTime.of(2021, 4, 23, 17, 44, 54, 761000000, ZoneId.of("GMT")).toInstant()
      assertThat(submissionListResponse.submissionInfoList[1].createdDate ?: Instant.now()).isEqualTo(expectedCreatedDate)

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
}