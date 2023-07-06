package ca.ewert.notarytoolkotlin.response

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Parent Class for all Notary Api Response Classes
 *
 * @property responseMetaData Meta information about the http response.
 * @author Victor Ewert
 */
open class NotaryApiResponse internal constructor(val responseMetaData: ResponseMetaData) {

  /** The timestamp of when the client received the Response */
  val receivedTimestamp: Instant = Instant.now()

  companion object {
    /**
     * Formatter for formatting HTTP Dates See: [https://http.dev/date](https://http.dev/date) for format details.
     * Used for parsing the Date in HTTP headers.
     */
    val HTTP_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O", Locale.US)
  }
}
