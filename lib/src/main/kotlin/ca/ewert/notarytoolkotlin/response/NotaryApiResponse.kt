package ca.ewert.notarytoolkotlin.response

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.MediaType
import okhttp3.Response
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Super class for all Notary Api Response Classes
 *
 * Created: 2023-06-15
 * @author vewert
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

  /**
   * Class for storing Response Metadata
   */
  class ResponseMetaData internal constructor(response: Response) {

    /** The HTTP Status Code, e.g. `400` */
    val httpStatusCode: Int

    /** The HTTP Status Message, e.g. `OK`  */
    val httpStatusMessage: String

    /** The Combined HTTP Status code and message, e.g. `400 - OK` */
    val httpStatusString: String

    /** Response Headers
     * //TODO Decide whether to keep
     */
    val headers: Map<String, String>

    /** Value of the `Date` Header */
    val headerDate: Instant?

    /** Response Content Type */
    val contentType: MediaType?

    /** Response Content Length, in bytes */
    val contentLength: Long?

    /** Response Content Body */
    val rawContents: String?

    init {
      log.info { "Inside init" }
      httpStatusCode = response.code
      httpStatusMessage = response.message
      httpStatusString = "$httpStatusCode - $httpStatusMessage"
      headers = response.headers.toMap()
      headerDate = response.headers.getInstant("Date")
      contentType = response.body?.contentType()
      contentLength = response.body?.contentLength()
      rawContents = response.body?.string()
      response.close()
    }
  }
}
