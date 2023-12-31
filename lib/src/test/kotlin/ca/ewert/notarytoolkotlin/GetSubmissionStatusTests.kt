package ca.ewert.notarytoolkotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import assertk.fail
import ca.ewert.notarytoolkotlin.response.Status
import ca.ewert.notarytoolkotlin.response.SubmissionId
import ca.ewert.notarytoolkotlin.response.SubmissionInfo
import ca.ewert.notarytoolkotlin.response.createMockResponse200
import ca.ewert.notarytoolkotlin.response.createMockResponse401
import ca.ewert.notarytoolkotlin.response.createMockResponse403
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
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val log = KotlinLogging.logger {}

/**
 * Unit tests for [NotaryToolClient.getSubmissionStatus]
 *
 * @author Victor Ewert
 */
class GetSubmissionStatusTests : NotaryToolClientTests() {

  /**
   * Tests getSubmissionStatus, using valid request to actual Apple Notary API
   */
  @Test
  @Tag("AppleServer")
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
  @Tag("AppleServer")
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

  /**
   * Tests getSubmissionStatus, using valid submissionId
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionStatus Valid submissionId Test")
  fun getSubmissionStatusValid() {
    val responseBody: String = """
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

    mockWebServer.enqueue(createMockResponse200(responseBody))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val expectedCreatedDate: Instant = ZonedDateTime.of(2022, 6, 8, 1, 38, 9, 498000000, ZoneId.of("UTC")).toInstant()

    val submissionIdResult = SubmissionId.of("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isOk()
      getSubmissionStatusResult.onSuccess { submissionStatusResponse ->
        assertThat(submissionStatusResponse.responseMetaData.httpStatusCode).isEqualTo(200)
        assertThat(submissionStatusResponse.responseMetaData.requestUrlString).isEqualTo(baseUrl.toString() + "submissions/2efe2717-52ef-43a5-96dc-0797e4ca1041")
        assertThat(submissionStatusResponse.submissionInfo.name).isEqualTo("OvernightTextEditor_11.6.8.zip")
        assertThat(submissionStatusResponse.submissionInfo).prop(SubmissionInfo::id).prop(SubmissionId::id)
          .isEqualTo("2efe2717-52ef-43a5-96dc-0797e4ca1041")
        assertThat(submissionStatusResponse.submissionInfo).prop(SubmissionInfo::createdDate)
          .isEqualTo(expectedCreatedDate)
        assertThat(submissionStatusResponse.submissionInfo).prop(SubmissionInfo::status)
          .isEqualTo(Status.ACCEPTED)
      }
    }
  }

  /**
   * Tests getSubmissionStatus, using invalid submissionId
   * Expect that a [NotaryToolError.UserInputError.InvalidSubmissionIdError] will occur
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionStatus Invalid submissionId Test")
  fun getSubmissionStatusInvalid() {
    val responseBody: String = """
    {
      "errors": [{
        "id": "5685647e-0125-4343-a068-1c5786499827",
        "status": "404",
        "code": "NOT_FOUND",
        "title": "The specified resource does not exist",
        "detail": "There is no resource of type 'submissions' with id '5685647e-0125-4343-a068-1c5786499827'"
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

    val submissionIdResult = SubmissionId.of("5685647e-0125-4343-a068-1c5786499827")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isErr()
      getSubmissionStatusResult.onFailure { notaryToolError ->
        assertThat(notaryToolError).isInstanceOf<NotaryToolError.UserInputError.InvalidSubmissionIdError>()
        log.info() { notaryToolError }
        assertThat(notaryToolError.msg).isEqualTo("There is no resource of type 'submissions' with id '5685647e-0125-4343-a068-1c5786499827'")
      }
    }
  }

  /**
   * Tests getSubmissionStatus, with an invalid url, giving a General 404 error.
   * Expect that a [NotaryToolError.HttpError.ClientError4xx] will occur
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionStatus 404")
  fun getSubmissionStatus404() {
    mockWebServer.enqueue(createMockResponse404General())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("5685647e-0125-4343-a068-1c5786499827")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isErr()
      getSubmissionStatusResult.onFailure { notaryToolError ->
        assertThat(notaryToolError).isInstanceOf<NotaryToolError.HttpError.ClientError4xx>()
        log.info() { notaryToolError }
        val httpError: NotaryToolError.HttpError = notaryToolError as NotaryToolError.HttpError.ClientError4xx
        assertThat(httpError.responseMetaData.httpStatusCode).isEqualTo(404)
      }
    }
  }

  /**
   * Tests making a request to getSubmissionStatus, with a 401 - Unauthenticated response.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionStatus 401 Test")
  fun getSubmissionStatus401() {
    mockWebServer.enqueue(createMockResponse401())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("5685647e-0125-4343-a068-1c5786499827")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isErr()
      getSubmissionStatusResult.onFailure { error ->
        when (error) {
          is NotaryToolError.UserInputError.AuthenticationError -> {
            log.info { "Authentication Error" }
            assertThat(error.msg).isEqualTo("Notary API Web Service could not authenticate the request. Please check that the issuer id, key identifier, and private key file are correct.")
          }

          else -> log.warn {
            error.msg
            fail(AssertionError("Incorrect Error: $error"))
          }
        }
      }
    }
  }

  /**
   * Tests making a request to getSubmissionStatus, with a 403 - Unauthorized response.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionStatus 403 Test")
  fun getSubmissionStatus403() {
    mockWebServer.enqueue(createMockResponse403())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("5685647e-0125-4343-a068-1c5786499827")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isErr()
      getSubmissionStatusResult.onFailure { error ->
        when (error) {
          is NotaryToolError.UserInputError.AuthenticationError -> {
            log.info { error }
          }

          else -> log.warn {
            error.msg
            fail(AssertionError("Incorrect Error: $error"))
          }
        }
      }
    }
  }

  /**
   * Tests making a request to getSubmissionStatus, with a 500 - Internal Server Error.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionStatus 500 Test")
  fun getSubmissionStatus500() {
    mockWebServer.enqueue(createMockResponse500())

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("5685647e-0125-4343-a068-1c5786499827")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isErr()
      getSubmissionStatusResult.onFailure { error ->
        when (error) {
          is NotaryToolError.HttpError.ServerError5xx -> {
            log.info { error }
          }

          else -> log.warn {
            error.msg
            fail(AssertionError("Incorrect Error: $error"))
          }
        }
      }
    }
  }

  /**
   * Tests making a request to getSubmissionStatus, simulating a connection issue
   * using [SocketPolicy.NO_RESPONSE]
   */
  @Test
  @Tag("MockServer")
  @DisplayName("getSubmissionStatus No Response Test")
  fun getSubmissionStatusNoResponse() {
    mockWebServer.enqueue(createMockResponseConnectionProblem(SocketPolicy.NO_RESPONSE))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("5685647e-0125-4343-a068-1c5786499827")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isErr()
      getSubmissionStatusResult.onFailure { error ->
        when (error) {
          is NotaryToolError.ConnectionError -> {
            log.info { error }
          }

          else -> {
            log.warn { error.msg }
            fail(AssertionError("Incorrect Error: $error"))
          }
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
  @DisplayName("getSubmissionStatus Disconnect During Response Test")
  fun getSubmissionStatusDisconnect() {
    mockWebServer.enqueue(createMockResponseConnectionProblem(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("5685647e-0125-4343-a068-1c5786499827")
    assertThat(submissionIdResult).isOk()
    submissionIdResult.onSuccess { submissionId ->
      val getSubmissionStatusResult = notaryToolClient.getSubmissionStatus(submissionId = submissionId)
      assertThat(getSubmissionStatusResult).isErr()
      getSubmissionStatusResult.onFailure { error ->
        when (error) {
          is NotaryToolError.ConnectionError -> {
            log.info { error }
          }

          else -> {
            log.warn { error.msg }
            fail(AssertionError("Incorrect Error: $error"))
          }
        }
      }
    }
  }

  /**
   * Tests polling where the final status is accepted.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Polling Accepted Test")
  fun pollStatusAccepted() {
    val responseBodyInProgress: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "In Progress"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    val responseBodyAccepted: String = """
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

    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyAccepted))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    submissionIdResult.onSuccess { submissionId ->
      val result = notaryToolClient.pollSubmissionStatus(
        submissionId = submissionId,
        maxPollCount = 10,
        delayFunction = { _: Int -> Duration.ofSeconds(10) },
        progressCallback = { currentPollCount, submissionStatusResponse ->
          println(
            "$currentPollCount - Current Status: ${submissionStatusResponse.submissionInfo.status} at ${
              submissionStatusResponse.receivedTimestamp.atZone(
                ZoneId.systemDefault(),
              )
            }}",
          )
        },
      )
      log.info { "Done" }
      result.onSuccess { submissionStatusResponse ->
        assertThat(submissionStatusResponse.submissionInfo.status).isEqualTo(Status.ACCEPTED)
      }

      result.onFailure { notaryToolError -> fail(AssertionError(notaryToolError.msg)) }
    }
  }

  /**
   * Tests polling where the final status is Rejected.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Polling Rejected Test")
  fun pollStatusRejected() {
    val responseBodyInProgress: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "In Progress"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    val responseBodyRejected: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "Rejected"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyRejected))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    submissionIdResult.onSuccess { submissionId ->
      val result = notaryToolClient.pollSubmissionStatus(
        submissionId = submissionId,
        maxPollCount = 10,
        delayFunction = { _: Int -> Duration.ofSeconds(5) },
        progressCallback = { currentPollCount, submissionStatusResponse ->
          println("$currentPollCount - Current Status: ${submissionStatusResponse.submissionInfo.status} at ${ZonedDateTime.now()}")
        },
      )
      log.info { "Done" }
      result.onSuccess { submissionStatusResponse ->
        assertThat(submissionStatusResponse.submissionInfo.status).isEqualTo(Status.REJECTED)
      }
      result.onFailure { notaryToolError -> fail(AssertionError(notaryToolError.msg)) }
    }
  }

  /**
   * Tests polling where the final status is Rejected.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Polling Invalid Test")
  fun pollStatusInvalid() {
    val responseBodyInProgress: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "In Progress"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    val responseBodyInvalid: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "Invalid"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInvalid))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    submissionIdResult.onSuccess { submissionId ->
      val result = notaryToolClient.pollSubmissionStatus(
        submissionId = submissionId,
        maxPollCount = 10,
        delayFunction = { _: Int -> Duration.ofSeconds(5) },
        progressCallback = { currentPollCount, submissionStatusResponse ->
          println("$currentPollCount - Current Status: ${submissionStatusResponse.submissionInfo.status} at ${ZonedDateTime.now()}")
        },
      )
      log.info { "Done" }
      result.onSuccess { submissionStatusResponse ->
        assertThat(submissionStatusResponse.submissionInfo.status).isEqualTo(Status.INVALID)
      }
      result.onFailure { notaryToolError -> fail(AssertionError(notaryToolError.msg)) }
    }
  }

  /**
   * Tests polling where an error occurs after several iterations.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Polling with Error Test")
  fun pollStatusError() {
    val responseBodyInProgress: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "In Progress"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    val responseBodyAccepted: String = """
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

    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse500())
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyAccepted))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    submissionIdResult.onSuccess { submissionId ->
      val result = notaryToolClient.pollSubmissionStatus(
        submissionId = submissionId,
        maxPollCount = 10,
        delayFunction = { _: Int -> Duration.ofSeconds(10) },
        progressCallback = { currentPollCount, submissionStatusResponse ->
          println(
            "$currentPollCount - Current Status: ${submissionStatusResponse.submissionInfo.status} at ${
              submissionStatusResponse.receivedTimestamp.atZone(
                ZoneId.systemDefault(),
              )
            }}",
          )
        },
      )
      log.info { "Done" }

      log.warn { result }

      result.onFailure { notaryToolError ->
        assertThat(notaryToolError is NotaryToolError.HttpError.ServerError5xx)
      }
    }
  }

  /**
   * Tests polling where polling times out after reaching maximum count.
   */
  @Test
  @Tag("MockServer")
  @DisplayName("Polling with Max Count Test")
  fun pollStatusMaxCountTest() {
    val responseBodyInProgress: String = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "In Progress"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))
    mockWebServer.enqueue(createMockResponse200(responseBodyInProgress))

    mockWebServer.start()
    val baseUrl: HttpUrl = mockWebServer.url("")

    val notaryToolClient = NotaryToolClient(
      privateKeyId = "A8B3X24VG1",
      issuerId = "70a7de6a-a537-48e3-a053-5a8a7c22a4a1",
      privateKeyFile = this.privateKeyFile!!,
      baseUrlString = baseUrl.toString(),
    )

    val submissionIdResult = SubmissionId.of("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    submissionIdResult.onSuccess { submissionId ->
      val result = notaryToolClient.pollSubmissionStatus(
        submissionId = submissionId,
        maxPollCount = 5,
        delayFunction = { _: Int -> Duration.ofSeconds(7) },
        progressCallback = { currentPollCount, submissionStatusResponse ->
          println(
            "$currentPollCount - Current Status: ${submissionStatusResponse.submissionInfo.status} at ${
              submissionStatusResponse.receivedTimestamp.atZone(
                ZoneId.systemDefault(),
              )
            }}",
          )
        },
      )
      log.warn { result }

      result.onFailure { notaryToolError ->
        assertThat(notaryToolError is NotaryToolError.PollingTimeout)
        assertThat(notaryToolError.msg).isEqualTo("Polling max count of 5, has been reached.")
      }
    }
  }
}
