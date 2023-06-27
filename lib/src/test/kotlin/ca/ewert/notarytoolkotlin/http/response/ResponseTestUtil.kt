package ca.ewert.notarytoolkotlin.http.response

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.mockwebserver.MockResponse
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val log = KotlinLogging.logger {}

/**
 * Utility functions for testing responses
 *
 * @author vewert
 */

/**
 * Creates a [MockResponse] with a status of 200 and all headers of an expected Response
 */
fun createMockResponse200(body: String): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  log.info {"Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(200)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Content-Type", "application/octet-stream")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Connection", "keep-alive")
    .addHeader("Set-Cookie", "dqsid=; Expires=Thu, 01 Jan 1970 00")
    .setBody(body)
}

/**
 * Creates a [MockResponse] with a status of 401 with headers and body matching Apples response.
 */
fun createMockResponse401(): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  val content = """
    Unauthenticated

    Request ID: TYPN6E62TY76LDJXYZTMLQRK7I.0.0
  """.trimIndent()
  log.info {"Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(401)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Content-Type", "text/plain")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Connection", "close")
    .setBody(content)
}