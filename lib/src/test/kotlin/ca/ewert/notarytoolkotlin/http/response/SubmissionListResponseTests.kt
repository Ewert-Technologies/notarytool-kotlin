package ca.ewert.notarytoolkotlin.http.response

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import ca.ewert.notarytoolkotlin.http.json.notaryapi.SubmissionListResponseJson
import mu.KotlinLogging
import okhttp3.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [SubmissionListResponse]
 *
 * @author vewert
 */
class SubmissionListResponseTests {
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
      if (response.isSuccessful) {
        val responseMetaData = NotaryApiResponse.ResponseMetaData(response)
        val jsonBody: String? = responseMetaData.rawContents
        assertThat(jsonBody).isNotNull()
        val submissionListResponseJson: SubmissionListResponseJson? = SubmissionListResponseJson.create(jsonBody)
        assertThat(submissionListResponseJson).isNotNull()
        if (submissionListResponseJson != null) {
          val submissionListResponse = SubmissionListResponse(responseMetaData, submissionListResponseJson)
          assertThat(submissionListResponse.submissionInfo).hasSize(3)
          assertThat(submissionListResponse.submissionInfo[1].id).isEqualTo("cf0c235a-dad2-4c24-96eb-c876d4cb3a2d")
          assertThat(submissionListResponse.submissionInfo[1].name).isEqualTo("OvernightTextEditor_11.6.7.zip")
          assertThat(submissionListResponse.submissionInfo[1].status).isEqualTo(SubmissionStatus.ACCEPTED)
        }
      } else {
        log.warn { "Request was not successful: $request" }
      }
    }
  }
}