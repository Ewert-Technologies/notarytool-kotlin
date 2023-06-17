package ca.ewert.notarytoolkotlin.http.response

import ca.ewert.notarytoolkotlin.http.json.notaryapi.SubmissionResponseJson
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Response from sending a request the `Get Submission Status` Endpoint.
 *
 * @author vewert
 */
class SubmissionResponse internal constructor(
  responseMetaData: ResponseMetaData,
  jsonResponse: SubmissionResponseJson
) :
  NotaryApiResponse(responseMetaData = responseMetaData) {

  /** The date that submission process was started */
  val createdDate: Instant

  /**
   * The name that was specified in the submissionName field of the Submit Software call when the submission
   * was started.
   */
  val name: String

  /** The status of the submission */
  val status: SubmissionStatus

  val id: String

  init {
    createdDate = Instant.parse(jsonResponse.submissionResponseData.attributes.createdDate)
    name = jsonResponse.submissionResponseData.attributes.name
    status = SubmissionStatus.fromString(jsonResponse.submissionResponseData.attributes.status)
    id = jsonResponse.submissionResponseData.id
  }
}