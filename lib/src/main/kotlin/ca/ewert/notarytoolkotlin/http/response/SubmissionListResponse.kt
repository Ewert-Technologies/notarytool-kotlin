package ca.ewert.notarytoolkotlin.http.response

import ca.ewert.notarytoolkotlin.http.json.notaryapi.SubmissionListResponseJson
import mu.KotlinLogging
import java.time.Instant
import java.time.format.DateTimeParseException

private val log = KotlinLogging.logger {}

/**
 * Response from sending a request the [Get Previous Submissions](https://developer.apple.com/documentation/notaryapi/get_previous_submissions)
 * Endpoint.
 *
 * @author vewert
 */
class SubmissionListResponse internal constructor(
  responseMetaData: ResponseMetaData,
  jsonResponse: SubmissionListResponseJson
) : NotaryApiResponse(responseMetaData) {

  /**
   * The notary service’s response to a request for information about your team’s previous submissions
   */
  val submissionInfoList: List<SubmissionInfo>

  init {
    submissionInfoList = jsonResponse.submissionListResponseData.map { submissionsDataJson ->
      val createdDateText = submissionsDataJson.attributes.createdDate
      val createdDate = try {
        Instant.parse(submissionsDataJson.attributes.createdDate)
      } catch (dateTimeParseException: DateTimeParseException) {
        log.warn("Error parsing 'createdDate' ($createdDateText) from Web API response, use ", dateTimeParseException)
        null
      }
      val name = submissionsDataJson.attributes.name

      val statusText: String = submissionsDataJson.attributes.status
      val status = SubmissionStatus.fromString(statusText)
      val id = submissionsDataJson.id

      SubmissionInfo(createdDate, createdDateText, name, status, statusText, id)
    }
  }
}