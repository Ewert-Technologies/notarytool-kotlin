package ca.ewert.notarytoolkotlin.http.response

import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionLogUrlResponseJson
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.MalformedURLException
import java.net.URL

private val log = KotlinLogging.logger {}

/**
 * Response from sending a request the [`Get Submission Log`](https://developer.apple.com/documentation/notaryapi/get_submission_log)
 * Endpoint.
 *
 * @author Victor Ewert
 */
class SubmissionLogUrlResponse internal constructor(
  responseMetaData: NotaryApiResponse.ResponseMetaData,
  jsonResponse: SubmissionLogUrlResponseJson,
) : NotaryApiResponse(responseMetaData = responseMetaData) {

  /**
   * The URL that can be used to download the logs for a submission.
   * The URL serves a JSON-encoded file that contains the log information.
   * The URL is valid for only a few hours. If you need the log again later, ask for the URL again
   * by making another call to the Get Submission Log endpoint.
   *
   * May be `null` if [developerLogUrlString] can't be converted to a [URL]
   */
  val developerLogUrl: URL?

  /**
   * The URL that can be used to download the logs for a submission, as a String
   */
  val developerLogUrlString: String

  /**
   * he unique identifier for this submission. This value matches the value passed in
   * when making the getSubmissionLog request.
   *
   * May be `null` if the submissionId String returned from the Notary API Web Service
   * is not a valid String.
   */
  val submissionId: SubmissionId

  init {
    this.developerLogUrlString = jsonResponse.submissionLogResponseData.attributes.developerLogUrl
    this.developerLogUrl = try {
      URL(this.developerLogUrlString)
    } catch (malformedUrlException: MalformedURLException) {
      log.warn(malformedUrlException) { "Error attempting to create URL from String: ${this.developerLogUrlString}" }
      null
    }
    submissionId = SubmissionId(jsonResponse.submissionLogResponseData.id)
  }
}
