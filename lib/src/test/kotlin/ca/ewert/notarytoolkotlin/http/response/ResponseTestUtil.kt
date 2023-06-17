package ca.ewert.notarytoolkotlin.http.response

import mu.KotlinLogging
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

fun createMockResponse(body: String): MockResponse {
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