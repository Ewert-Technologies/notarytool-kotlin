package ca.ewert.notarytoolkotlin.json.notaryapi

import ca.ewert.notarytoolkotlin.NotaryToolError
import ca.ewert.notarytoolkotlin.i18n.ErrorStringsResource
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

/**
 * Top-level Response when there is an error.
 *
 * Instance can be created by parsing json response
 *
 * @property errors A list of the [ErrorJson] responses.
 * @author Victor Ewert
 */
data class ErrorResponseJson(
  val errors: List<ErrorJson>,
) {
  companion object {

    /**
     * Creates a [ErrorResponseJson] from the json String
     *
     * @param jsonString The json String to parse
     * @return A [ErrorResponseJson] or a [NotaryToolError.JsonParseError]
     */
    @JvmStatic
    fun create(jsonString: String?): Result<ErrorResponseJson, NotaryToolError.JsonParseError> {
      return if (!jsonString.isNullOrEmpty()) {
        val jsonAdapter: JsonAdapter<ErrorResponseJson> =
          moshi.adapter(ErrorResponseJson::class.java).failOnUnknown().lenient()
        try {
          val errorResponseJson: ErrorResponseJson? = jsonAdapter.fromJson(jsonString)
          if (errorResponseJson != null) {
            Ok(errorResponseJson)
          } else {
            val msg = ErrorStringsResource.getString("json.parse.other.error")
            Err(NotaryToolError.JsonParseError(msg, jsonString))
          }
        } catch (jsonDataException: JsonDataException) {
          val msg = ErrorStringsResource.getString("json.parse.error").format(jsonDataException.localizedMessage)
          Err(NotaryToolError.JsonParseError(msg, jsonString))
        }
      } else {
        val msg = ErrorStringsResource.getString("json.parse.null.blank.error")
        Err(NotaryToolError.JsonParseError(msg, jsonString = jsonString))
      }
    }
  }
}

/**
 * Error data from the Response.
 *
 * @property code The HTTP response code message.
 * @property detail Details of the error.
 * @property id The submissionId used in the request.
 * @property status The HTTP status code.
 * @property title The error message title.
 * @author Victor Ewert
 */
data class ErrorJson(
  val code: String,
  val detail: String,
  val id: String,
  val status: String,
  val title: String,
)
