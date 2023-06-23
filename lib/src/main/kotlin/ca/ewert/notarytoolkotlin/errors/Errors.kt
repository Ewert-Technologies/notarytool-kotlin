package ca.ewert.notarytoolkotlin.errors

/**
 * Parent class of all Error types
 *
 * @author vewert
 */
sealed interface NotaryToolError {
  val msg: String

  /**
   * @property msg The error message
   * @property jsonString The json String that was used when attempting to parse
   */
  data class JsonParseError(override val msg: String, val jsonString: String?) : NotaryToolError

  data class GeneralError(override val msg: String) : NotaryToolError

  data class HttpError(override val msg: String, val httpStatusCode: Int, val httpStatusMsg: String,
                       val requestUrl: String = "", val bodyContent: String?) : NotaryToolError
}

sealed interface JsonWebTokenError : NotaryToolError {
  data class PrivateKeyNotFound(override val msg: String) : JsonWebTokenError
  data class TokenCreationError(override val msg: String) : JsonWebTokenError
}



