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
open class NotaryToolClientTests {

  protected val privateKeyFile: Path? = resourceToPath("/private/AuthKey_Test.p8")

  protected var mockWebServer: MockWebServer

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
}
