package ca.ewert.notarytoolkotlin.http.response

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import ca.ewert.notarytoolkotlin.http.json.notaryapi.SubmissionResponseJson
import ca.ewert.notarytoolkotlin.isCloseTo
import ca.ewert.notarytoolkotlin.isOk
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [SubmissionStatusResponse]
 * @author vewert
 */
class SubmissionResponseTests() {
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
  fun basicConnectionTest() {
    assertThat(mockWebServer).isNotNull()

    val body: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "Accepted"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(body))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("/notary/v2/submissions")

    val request = Request.Builder()
      .url(baseUrl)
      .get()
      .build()

    val client = OkHttpClient.Builder()
      .protocols(listOf(Protocol.HTTP_1_1, Protocol.HTTP_2))
      .build()

    client.newCall(request).execute().use { response: Response ->
      log.info("Returned Response: $response")
      assertThat(response.isSuccessful).isTrue()
      if (!response.isSuccessful) {
        log.warn { "Request was not successful: $request" }
      }
    }
  }

  @Test
  fun test1() {
    assertThat(mockWebServer).isNotNull()

    val body: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "Accepted"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(body))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("/notary/v2/submissions")

    val request = Request.Builder()
      .url(baseUrl)
      .header("User-Agent", "notarytool-kotlin/0.1.0")
      .get()
      .build()

    val client = OkHttpClient.Builder().build()

    client.newCall(request).execute().use { response: Response ->
      log.info("Returned Response: $response")
      assertThat(response.isSuccessful).isTrue()
      if (response.isSuccessful) {
        val responseMetaData = NotaryApiResponse.ResponseMetaData(response)
        val jsonBody: String? = responseMetaData.rawContents
        assertThat(jsonBody).isNotNull()
        val submissionResponseJsonResult = SubmissionResponseJson.create(jsonBody)

        assertThat(submissionResponseJsonResult).isOk()

        submissionResponseJsonResult.onSuccess { submissionResponseJson ->
          val submissionResponse = SubmissionStatusResponse(responseMetaData, jsonResponse = submissionResponseJson)
          assertThat(submissionResponse.receivedTimestamp.atZone(ZoneId.systemDefault())).isCloseTo(
            ZonedDateTime.now(),
            Duration.of(500, ChronoUnit.MILLIS),
          )

          val expectedCreatedDate: ZonedDateTime = ZonedDateTime.of(2022, 6, 8, 1, 38, 9, 498000000, ZoneId.of("Z"))
          assertThat(submissionResponse.submissionInfo.createdDate).isEqualTo(expectedCreatedDate.toInstant())
          assertThat(submissionResponse.submissionInfo.id).isEqualTo(SubmissionId("2efe2717-52ef-43a5-96dc-0797e4ca1041"))
          assertThat(submissionResponse.submissionInfo.status).isEqualTo(SubmissionStatus.ACCEPTED)
          assertThat(submissionResponse.submissionInfo.name).isEqualTo("OvernightTextEditor_11.6.8.zip")
          log.info { "header date: ${submissionResponse.responseMetaData.headerDate?.atZone(ZoneId.systemDefault())}" }
          log.info { "content-type: ${submissionResponse.responseMetaData.contentType}" }
          assertThat(submissionResponse.responseMetaData.contentType).isEqualTo("application/octet-stream".toMediaTypeOrNull())
          log.info { "content-length ${submissionResponse.responseMetaData.contentLength}" }
          assertThat(submissionResponse.responseMetaData.contentLength).isEqualTo(submissionResponse.responseMetaData.rawContents?.length?.toLong())

          val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
          log.info { "Recorded Request: $recordedRequest" }
          log.info { "Recorded Request Headers: ${recordedRequest.headers}" }
          log.info { "Recorded Request User-Agent ${recordedRequest.getHeader("User-Agent")}" }
        }
      } else {
        log.warn { "Request was not successful: $request" }
      }
    }
  }
}
