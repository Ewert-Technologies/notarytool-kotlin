package ca.ewert.notarytoolkotlin.json.notaryapi

import com.squareup.moshi.Json

/**
 * Top level Response from making a **`Get Submission Log`** Request.
 * Corresponds to [`SubmissionLogURLResponse`](https://developer.apple.com/documentation/notaryapi/newsubmissionresponse),
 * from Apple's API. _The notary service’s response to a request for the log information about a completed submission_
 *
 * @property submissionLogResponseData `SubmissionLogURLResponse.Data` Data that indicates how to get the
 * log information for a particular submission.
 * @property meta `SubmissionLogURLResponse.Meta` An empty object that you can ignore.
 */
data class SubmissionLogUrlResponseJson(
  @Json(name = "data") val submissionLogResponseData: SubmissionsLogDataJson,
  val meta: SubmissionsLogMetaJson,
)

/**
 * Corresponds to [`SubmissionLogURLResponse.Data`](https://developer.apple.com/documentation/notaryapi/submissionlogurlresponse/data),
 * from Apple's API. _Data that indicates how to get the log information for a particular submission._
 *
 * @property attributes `SubmissionLogURLResponse.Data.Attributes` Information about the log
 * associated with the submission.
 * @property id The unique identifier for this submission. This value matches the value that you provided as a path
 * parameter to the Get Submission Log call that elicited this response
 * @property type The resource type.
 */
data class SubmissionsLogDataJson(
  val attributes: SubmissionsLogAttributesJson,
  val id: String,
  val type: String,
)

/**
 * Corresponds to [`SubmissionLogURLResponse.Data.Attributes`](https://developer.apple.com/documentation/notaryapi/submissionlogurlresponse/data/attributes),
 * from Apples API. _Information about the log associated with the submission._
 *
 * @property developerLogUrl The URL that you use to download the logs for a submission.
 * The URL serves a JSON-encoded file that contains the log information. The URL is valid for
 * only a few hours. If you need the log again later, ask for the URL again by making
 * another call to the Get Submission Log endpoint.
 */
data class SubmissionsLogAttributesJson(
  val developerLogUrl: String,
)

/**
 * Corresponds to [`SubmissionLogURLResponse.Meta`](https://developer.apple.com/documentation/notaryapi/submissionlogurlresponse/meta),
 * from Apple's API. _An empty object. This object is reserved for future use._
 */
class SubmissionsLogMetaJson
