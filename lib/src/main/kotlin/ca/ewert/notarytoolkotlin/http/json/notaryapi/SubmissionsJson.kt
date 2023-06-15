package ca.ewert.notarytoolkotlin.http.json.notaryapi

import com.squareup.moshi.Json


/**
 * Top level Response from making a **`Get Submission Status`** Request.
 * Corresponds to [`SubmissionResponse`](https://developer.apple.com/documentation/notaryapi/submissionresponse),
 * from Apple's API. _You receive a structure of this type in response to a call to
 * the `Get Submission Status` endpoint._
 *
 * @property submissionResponseData `SubmissionResponse.Data` Data that describes the status of the submission request.
 * @property meta `SubmissionResponse.Meta` An empty object that you can ignore.
 */
data class SubmissionResponseJson(
  @Json(name = "data") val submissionResponseData: SubmissionsDataJson,
  val meta: SubmissionsMetaJson
)

/**
 * Top level Response from making a **`Get Previous Submissions`** Request.
 * Corresponds to [`SubmissionsListResponse`](https://developer.apple.com/documentation/notaryapi/submissionlistresponse),
 * from Apple's API. _You receive a structure of this type in response to a call to the
 * `Get Previous Submissions` endpoint. The list includes only the 100 most recent submissions._
 *
 * @property submissionListResponseData `SubmissionListResponse.Data` An array of objects, each of which describes
 * one of your team’s previous submissions.
 * @property meta `SubmissionListResponse.Meta` An empty object that you can ignore.
 */
data class SubmissionListResponseJson(
  @Json(name = "data") val submissionListResponseData: List<SubmissionsDataJson>,
  val meta: SubmissionsMetaJson
)

/**
 * Corresponds to [`SubmissionResponse.Data`](https://developer.apple.com/documentation/notaryapi/submissionresponse/data),
 * and to [`SubmissionListResponse.Data`](https://developer.apple.com/documentation/notaryapi/submissionlistresponse/data),
 * from Apples API. _Information that the service provides about the status of a notarization submission /
 * Data that describes one of your team’s previous submissions._
 *
 * @property attributes Information about (the status) of a particular submission.
 * @property id The unique identifier for this submission.
 * @property type The resource type.
 */
data class SubmissionsDataJson(
  val attributes: SubmissionsAttributesJson,
  val id: String,
  val type: String
)


/**
 * Corresponds to [`SubmissionResponse.Data.Attributes`](https://developer.apple.com/documentation/notaryapi/submissionresponse/data/attributes),
 * and to [`SubmissionListResponse.Data.Attributes`](https://developer.apple.com/documentation/notaryapi/submissionlistresponse/data/attributes),
 * from Apples API. _Information about the status of a submission._
 *
 * @property createdDate The date that you started the submission process, given in ISO 8601 format,
 * like `2022-06-08T01:38:09.498Z`.
 * @property name The name that you specified in the submissionName field of the Submit Software call
 * when you started the submission.
 * @property status The status of the submission. The associated string contains one of the following:
 * `Accepted`, `In Progress`, `Invalid`, or `Rejected`.
 */
data class SubmissionsAttributesJson(
  val createdDate: String,
  val name: String,
  val status: String
)

/**
 * Corresponds to [`SubmissionResponse.Meta`](https://developer.apple.com/documentation/notaryapi/submissionresponse/meta),
 * and to [`SubmissionListResponse.Meta`](https://developer.apple.com/documentation/notaryapi/submissionlistresponse/meta),
 * from Apples API. _An empty object. This object is reserved for future use._
 */
class SubmissionsMetaJson