package ca.ewert.notarytoolkotlin.json.notaryapi

import ca.ewert.notarytoolkotlin.NotaryToolError
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
 * Top level Response from making a [**`Get Submission Log`**](https://developer.apple.com/documentation/notaryapi/get_submission_log)
 * Request.
 * Corresponds to [`SubmissionLogURLResponse`](https://developer.apple.com/documentation/notaryapi/newsubmissionresponse),
 * from Apple's API. _The notary service's response to a request for the log information about a completed submission_
 *
 * @property submissionLogResponseData `SubmissionLogURLResponse.Data` Data that indicates how to get the
 * log information for a particular submission.
 * @property meta `SubmissionLogURLResponse.Meta` An empty object that you can ignore.
 */
data class SubmissionLogUrlResponseJson internal constructor(
  @Json(name = "data") val submissionLogResponseData: SubmissionLogUrlResponseDataJson,
  val meta: SubmissionLogUrlResponseMetaJson,
) {
  companion object {

    /**
     * Creates a [SubmissionLogUrlResponseJson] from the json String.
     *
     * @return A [SubmissionLogUrlResponseJson] or a [NotaryToolError.JsonParseError]
     */
    @JvmStatic
    fun create(jsonString: String?): Result<SubmissionLogUrlResponseJson, NotaryToolError.JsonParseError> {
      return if (!jsonString.isNullOrEmpty()) {
        val jsonAdapter: JsonAdapter<SubmissionLogUrlResponseJson> =
          moshi.adapter(SubmissionLogUrlResponseJson::class.java).failOnUnknown().lenient()
        try {
          val submissionLogUrlResponseJson = jsonAdapter.fromJson(jsonString)
          if (submissionLogUrlResponseJson != null) {
            Ok(submissionLogUrlResponseJson)
          } else {
            val msg = ErrorStringsResource.getString("json.parse.other.error")
            Err(NotaryToolError.JsonParseError(msg = msg, jsonString = jsonString))
          }
        } catch (jsonDataException: JsonDataException) {
          val msg = ErrorStringsResource.getString("json.parse.error").format(jsonDataException.message)
          Err(NotaryToolError.JsonParseError(msg = msg, jsonString = jsonString))
        }
      } else {
        val msg = ErrorStringsResource.getString("json.parse.null.blank.error")
        Err(NotaryToolError.JsonParseError(msg = msg, jsonString = jsonString))
      }
    }
  }
}

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
data class SubmissionLogUrlResponseDataJson internal constructor(
  val attributes: SubmissionLogUrlResponseAttributesJson,
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
data class SubmissionLogUrlResponseAttributesJson(
  val developerLogUrl: String,
)

/**
 * Corresponds to [`SubmissionLogURLResponse.Meta`](https://developer.apple.com/documentation/notaryapi/submissionlogurlresponse/meta),
 * from Apple's API. _An empty object. This object is reserved for future use._
 */
class SubmissionLogUrlResponseMetaJson
