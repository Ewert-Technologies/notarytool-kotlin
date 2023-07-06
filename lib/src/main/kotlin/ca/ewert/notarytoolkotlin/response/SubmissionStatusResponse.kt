package ca.ewert.notarytoolkotlin.response

import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionResponseJson
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.format.DateTimeParseException

private val log = KotlinLogging.logger {}

/**
 * Response from sending a request the [`Get Submission Status`](https://developer.apple.com/documentation/notaryapi/get_submission_status)
 * Endpoint.
 *
 * @param responseMetaData Metadata received from the response.
 * @param jsonResponse The response body json object.
 * @author Victor Ewert
 */
class SubmissionStatusResponse internal constructor(
  responseMetaData: ResponseMetaData,
  jsonResponse: SubmissionResponseJson,
) : NotaryApiResponse(responseMetaData = responseMetaData) {

  /** Information about the status of a submission */
  val submissionInfo: SubmissionInfo

  init {
    val createdDateText = jsonResponse.submissionResponseData.attributes.createdDate
    val createdDate = try {
      Instant.parse(jsonResponse.submissionResponseData.attributes.createdDate)
    } catch (dateTimeParseException: DateTimeParseException) {
      log.warn(dateTimeParseException) { "Error parsing 'createdDate' ($createdDateText) from Web API response, use createdDateText instead" }
      null
    }
    val name = jsonResponse.submissionResponseData.attributes.name

    val statusText: String = jsonResponse.submissionResponseData.attributes.status
    val status = Status.fromString(statusText)
    val id = jsonResponse.submissionResponseData.id

    submissionInfo = SubmissionInfo(createdDate, createdDateText, name, status, statusText, SubmissionId(id))
  }
}
