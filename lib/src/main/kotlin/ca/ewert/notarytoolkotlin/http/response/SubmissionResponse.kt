package ca.ewert.notarytoolkotlin.http.response

import ca.ewert.notarytoolkotlin.http.json.notaryapi.SubmissionResponseJson
import mu.KotlinLogging
import java.time.Instant
import java.time.format.DateTimeParseException

private val log = KotlinLogging.logger {}

/**
 * Response from sending a request the `Get Submission Status` Endpoint.
 *
 * @author vewert
 */
class SubmissionResponse internal constructor(
  responseMetaData: ResponseMetaData,
  jsonResponse: SubmissionResponseJson
) : NotaryApiResponse(responseMetaData = responseMetaData) {


  /** Information about the status of a submission */
  val submissionInfo: SubmissionInfo


  init {
    val createdDateText = jsonResponse.submissionResponseData.attributes.createdDate
    val createdDate = try {
      Instant.parse(jsonResponse.submissionResponseData.attributes.createdDate)
    } catch (dateTimeParseException: DateTimeParseException) {
      log.warn("Error parsing 'createdDate' ($createdDateText) from Web API response, use ", dateTimeParseException)
      null
    }
    val name = jsonResponse.submissionResponseData.attributes.name
    val status = SubmissionStatus.fromString(jsonResponse.submissionResponseData.attributes.status)
    val id = jsonResponse.submissionResponseData.id

    submissionInfo = SubmissionInfo(createdDate, createdDateText, name, status, id)
  }
}