package ca.ewert.notarytoolkotlin.response

import ca.ewert.notarytoolkotlin.json.notaryapi.ErrorJson
import ca.ewert.notarytoolkotlin.json.notaryapi.ErrorResponseJson

/**
 * An Error response, returned from the Notary API.
 *
 * @author Victor Ewert
 */
class ErrorResponse internal constructor(
  responseMetaData: ResponseMetaData,
  jsonResponse: ErrorResponseJson,
) : NotaryApiResponse(responseMetaData = responseMetaData) {

  /**
   * List of individual Errors.
   */
  val errorList: List<ErrorInfo>

  init {
    errorList = jsonResponse.errors.map { errorJson: ErrorJson -> ErrorInfo(errorJson) }
  }
}

/**
 * Represents an error, returned by the Notary API
 */
data class ErrorInfo internal constructor(val errorJson: ErrorJson) {

  /** The submssionId used in the request. */
  val id: String

  /** The status for the error. */
  val status: Int?

  /** The status for the error as a String, e.g. `"404"`. */
  val statusString: String

  /** The Error code, e.g. `"NOT_FOUND"`.  */
  val code: String

  /** The error title. */
  val title: String

  /** Detailed message for the error. */
  val detail: String

  init {
    this.id = errorJson.id
    this.status = errorJson.status.toIntOrNull()
    this.statusString = errorJson.status
    this.code = errorJson.code
    this.title = errorJson.title
    this.detail = errorJson.detail
  }
}
