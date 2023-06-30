package ca.ewert.notarytoolkotlin

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Protocol
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path

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
