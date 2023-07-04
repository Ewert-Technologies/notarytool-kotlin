package ca.ewert.notarytoolkotlin.json.notaryapi

import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import ca.ewert.notarytoolkotlin.i18n.ErrorStringsResource
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

/**
 * Top level Response from making a **`Get Submission Status`** Request.
 * Corresponds to [`SubmissionResponse`](https://developer.apple.com/documentation/notaryapi/submissionresponse),
 * from Apple's API. _You receive a structure of this type in response to a call to
 * the `Get Submission Status` endpoint._
 *
 * Instances can be created by parsing the json response
 *
 * @property submissionResponseData `SubmissionResponse.Data` Data that describes the status of the submission request.
 * @property meta `SubmissionResponse.Meta` An empty object that you can ignore.
 */
data class SubmissionResponseJson(
  @Json(name = "data") val submissionResponseData: SubmissionsDataJson,
  val meta: SubmissionsMetaJson,
) {
  companion object {

    /**
     * Creates a [SubmissionResponseJson] from the json String.
     *
     * @return A [SubmissionResponseJson] or a [NotaryToolError.JsonParseError]
     */
    @JvmStatic
    fun create(jsonString: String?): Result<SubmissionResponseJson, NotaryToolError.JsonParseError> {
      return if (!jsonString.isNullOrEmpty()) {
        val jsonAdapter: JsonAdapter<SubmissionResponseJson> =
          moshi.adapter(SubmissionResponseJson::class.java).failOnUnknown().lenient()
        try {
          val submissionResponseJson = jsonAdapter.fromJson(jsonString)
          if (submissionResponseJson != null) {
            Ok(submissionResponseJson)
          } else {
            val msg = ErrorStringsResource.getString("json.parse.other.error")
            Err(NotaryToolError.JsonParseError(msg = msg, jsonString = jsonString))
          }
        } catch (jsonDataException: JsonDataException) {
          val msg = ErrorStringsResource.getString("json.parse.error".format(jsonDataException.message))
          Err(NotaryToolError.JsonParseError(msg = msg, jsonString = jsonString))
        }
      } else {
        val msg = ErrorStringsResource.getString("json.parse.null.blank.error")
        Err(NotaryToolError.JsonParseError(msg = "msg", jsonString = jsonString))
      }
    }
  }
}

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
  val meta: SubmissionsMetaJson,
) {
  companion object {

    /**
     * Creates a [SubmissionListResponseJson] from the json String.
     *
     * @return A [SubmissionListResponseJson] or a [NotaryToolError.JsonParseError]
     */
    @JvmStatic
    fun create(jsonString: String?): Result<SubmissionListResponseJson, NotaryToolError.JsonParseError> {
      return if (!jsonString.isNullOrEmpty()) {
        val jsonAdapter: JsonAdapter<SubmissionListResponseJson> =
          moshi.adapter(SubmissionListResponseJson::class.java).failOnUnknown().lenient()
        try {
          val submissionListResponseJson = jsonAdapter.fromJson(jsonString)
          if (submissionListResponseJson != null) {
            Ok(submissionListResponseJson)
          } else {
            val msg = ErrorStringsResource.getString("json.parse.other.error")
            Err(NotaryToolError.JsonParseError(msg = msg, jsonString = jsonString))
          }
        } catch (jsonDataException: JsonDataException) {
          val msg = ErrorStringsResource.getString("json.parse.error".format(jsonDataException.message))
          Err(NotaryToolError.JsonParseError(msg = msg, jsonString = jsonString))
        }
      } else {
        val msg = ErrorStringsResource.getString("json.parse.null.blank.error")
        Err(NotaryToolError.JsonParseError(msg = "msg", jsonString = jsonString))
      }
    }
  }
}

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
  val type: String,
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
  val status: String,
)

/**
 * Corresponds to [`SubmissionResponse.Meta`](https://developer.apple.com/documentation/notaryapi/submissionresponse/meta),
 * and to [`SubmissionListResponse.Meta`](https://developer.apple.com/documentation/notaryapi/submissionlistresponse/meta),
 * from Apples API. _An empty object. This object is reserved for future use._
 */
class SubmissionsMetaJson
