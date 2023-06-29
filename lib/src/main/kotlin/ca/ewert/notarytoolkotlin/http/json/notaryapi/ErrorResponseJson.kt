package ca.ewert.notarytoolkotlin.http.json.notaryapi

import ca.ewert.notarytoolkotlin.errors.NotaryToolError
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
            Err(NotaryToolError.JsonParseError("Error creating Json Object.", jsonString))
          }
        } catch (jsonDataException: JsonDataException) {
          Err(NotaryToolError.JsonParseError("Error parsing json: ${jsonDataException.message}.", jsonString))
        }
      } else {
        Err(NotaryToolError.JsonParseError("Json String is <null> or empty.", jsonString = jsonString))
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
