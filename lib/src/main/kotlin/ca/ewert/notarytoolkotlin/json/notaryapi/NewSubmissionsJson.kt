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
 * Top level Request for making a **`Submit Software`** Request.
 * Corresponds to [`NewSubmissionRequest`](https://developer.apple.com/documentation/notaryapi/newsubmissionrequest),
 * from the Notary API. _Data that you provide when starting a submission to the notary service._
 *
 * @property notifications `NewSubmissionRequest.Notifications` An optional array of notifications that
 * you want to receive when notarization finishes. Omit this key if you don’t need a notification.
 * @property sha256 (Required) A cryptographic hash of the software that you want to notarize,
 * computed using Secure Hashing Algorithm 2 (SHA-2) with a 256-bit digest.
 * Supply the hash as a string of 64 hexadecimal digits. You must compute the hash from the
 * exact version of the software that you plan to upload to Amazon S3. Value: `/[A-Fa-f0-9]{64/`
 * @property submissionName (Required) The name of the file that you plan to submit.
 * The service includes this name in its responses when you ask for the status of a submission,
 * get a list of previous submissions, or get a log file corresponding to a submission.
 * The file name doesn't have to be unique among all your submissions, but making it so might help
 * you to distinguish among submissions in service responses.
 */
data class NewSubmissionRequestJson(
  val notifications: List<NewSubmissionRequestNotificationJson>,
  val sha256: String,
  val submissionName: String,
) {
  companion object {

    /**
     * Takes a [NewSubmissionAttributesJson] object and attempts to serialize it
     * to a json String.
     *
     * @param newSubmissionRequestJson
     */
    @JvmStatic
    internal fun toJsonString(newSubmissionRequestJson: NewSubmissionRequestJson): Result<String, NotaryToolError.JsonCreateError> {
      val jsonAdapter: JsonAdapter<NewSubmissionRequestJson> =
        moshi.adapter(NewSubmissionRequestJson::class.java).failOnUnknown().lenient()
      return try {
        Ok(jsonAdapter.toJson(newSubmissionRequestJson))
      } catch (jsonDataException: JsonDataException) {
        val msg = ErrorStringsResource.getString("json.create.error").format(jsonDataException.localizedMessage)
        Err(NotaryToolError.JsonCreateError(msg, newSubmissionRequestJson.toString()))
      }
    }
  }
}

/**
 * Corresponds to [`NewSubmissionRequest.Notifications`](https://developer.apple.com/documentation/notaryapi/newsubmissionrequest/notifications),
 * from Apples API. _A notification that the notary service sends you when notarization finishes._
 *
 * @property channel The channel that the service uses to notify you when notarization completes. The only supported value for this key is webhook.
 * @property target The URL that the notary service accesses when notarization completes.
 */
data class NewSubmissionRequestNotificationJson(
  val channel: String,
  val target: String,
)

/**
 * Top level Response from making a **`Submit Software`** Request.
 * Corresponds to [`NewSubmissionResponse`](https://developer.apple.com/documentation/notaryapi/newsubmissionresponse),
 * from the Notary API. _The notary service's response to a software submission._
 *
 * @property newSubmissionResponseData `NewSubmissionResponse.Data` Data that describes the result of the
 * submission request.
 * @property meta `NewSubmissionResponse.Meta` An empty object that you can ignore.
 */
data class NewSubmissionResponseJson(
  @Json(name = "data") val newSubmissionResponseData: NewSubmissionDataJson,
  val meta: NewSubmissionMetaJson,
) {
  companion object {

    /**
     * Creates a [NewSubmissionResponseJson] from the json String.
     *
     * @return A [NewSubmissionResponseJson] or a [NotaryToolError.JsonParseError]
     */
    @JvmStatic
    fun create(jsonString: String?): Result<NewSubmissionResponseJson, NotaryToolError.JsonParseError> {
      return if (!jsonString.isNullOrEmpty()) {
        val jsonAdapter: JsonAdapter<NewSubmissionResponseJson> =
          moshi.adapter(NewSubmissionResponseJson::class.java).failOnUnknown().lenient()
        try {
          val newSubmissionResponseJson = jsonAdapter.fromJson(jsonString)
          if (newSubmissionResponseJson != null) {
            Ok(newSubmissionResponseJson)
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
 * Corresponds to [`NewSubmissionResponse.Data`](https://developer.apple.com/documentation/notaryapi/newsubmissionresponse/data),
 * from Apples API. _Information that the notary service provides for uploading your software for notarization and tracking the submission._
 *
 * @property attributes `NewSubmissionResponse.Data.Attributes` Information that you use to upload your software to Amazon S3.
 * @property id A unique identifier for this submission. Use this value to track the status of your submission.
 * @property type The resource type.
 */
data class NewSubmissionDataJson(
  val attributes: NewSubmissionAttributesJson,
  val id: String,
  val type: String,
)

/**
 * Corresponds to [`NewSubmissionResponse.Data.Attributes`](https://developer.apple.com/documentation/notaryapi/newsubmissionresponse/data/attributes),
 * from Apples API. _Information that you use to upload your software for notarization._
 *
 * @property awsAccessKeyId An access key that you use in a call to Amazon S3.
 * @property awsSecretAccessKey A secret key that you use in a call to Amazon S3.
 * @property awsSecretAccessKey A session token that you use in a call to Amazon S3.
 * @property bucket The Amazon S3 bucket that you upload your software into.
 * @property objectKey The object key that identifies your software upload within the bucket.
 */
data class NewSubmissionAttributesJson(
  val awsAccessKeyId: String,
  val awsSecretAccessKey: String,
  val awsSessionToken: String,
  val bucket: String,
  @Json(name = "object") val objectKey: String,
)

/**
 * Corresponds to [`NewSubmissionResponse.Meta`](https://developer.apple.com/documentation/notaryapi/newsubmissionresponse/meta),
 * from Apples API. _An empty object. This object is reserved for future use._
 */
class NewSubmissionMetaJson
