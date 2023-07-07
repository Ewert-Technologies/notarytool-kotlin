package ca.ewert.notarytoolkotlin.response

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Response
import java.time.Instant

private val log = KotlinLogging.logger {}

/**
 * Class for accessing Response Metadata.
 *
 * @author Victor Ewert
 */
class ResponseMetaData internal constructor(response: Response) {

  /** The HTTP Status Code, e.g. `200` */
  val httpStatusCode: Int

  /** The HTTP Status Message, e.g. `OK`  */
  val httpStatusMessage: String

  /** The Combined HTTP Status code and message, e.g. `200 - OK` */
  val httpStatusString: String

  /** A map of all the Response Headers */
  val headers: Map<String, String>

  /** Value of the `Date` Header, if available */
  val headerDate: Instant?

  /** Response Content Type, if available */
  val contentType: String?

  /** Response Content Length (in bytes), if available */
  val contentLength: Long?

  /** Response Content Body, if available */
  val rawContents: String?

  init {
    log.info { "Inside init" }
    httpStatusCode = response.code
    httpStatusMessage = response.message
    httpStatusString = "$httpStatusCode - $httpStatusMessage"
    headers = response.headers.toMap()
    headerDate = response.headers.getInstant("Date")
    contentType = response.body?.contentType()?.toString()
    contentLength = response.body?.contentLength()
    rawContents = response.body?.string()
    response.close()
  }

  /**
   * A String representation of the object.
   * Displays as: `statusCode -  statusMessage; contentType`.
   *
   * Example: `"200 - OK; content-type: 'text/plain'"`
   */
  override fun toString(): String {
    return "$httpStatusString; content-type: '$contentType'"
  }
}