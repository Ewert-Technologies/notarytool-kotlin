package ca.ewert.notarytoolkotlin.errors

/**
 * Parent class of all Error types
 *
 * @author vewert
 */
sealed interface NotaryToolError {
  val msg: String

  sealed interface JsonWebTokenError : NotaryToolError {
    data class PrivateKeyNotFound(override val msg: String) : JsonWebTokenError
    data class TokenCreationError(override val msg: String) : JsonWebTokenError
  }

  sealed interface HttpError: NotaryToolError {
    val httpStatusCode: Int
    val httpStatusMsg: String
    val requestUrl: String
    val contentBody: String?

    data class InvalidSubmissionId (override val msg: String, override val httpStatusCode: Int = 404,
                                   override val httpStatusMsg: String, override val requestUrl: String = "",
                                   override val contentBody: String?) : HttpError

    data class ClientError4xx(override val msg: String, override val httpStatusCode: Int,
                              override val httpStatusMsg: String, override val requestUrl: String = "",
                              override val contentBody: String?) : HttpError

    data class ServerError5xx(override val msg: String, override val httpStatusCode: Int,
                              override val httpStatusMsg: String, override val requestUrl: String = "",
                              override val contentBody: String?) : HttpError

    data class OtherError(override val msg: String, override val httpStatusCode: Int,
                              override val httpStatusMsg: String, override val requestUrl: String = "",
                              override val contentBody: String?) : HttpError

  }

  /**
   * @property msg The error message
   * @property jsonString The json String that was used when attempting to parse
   */
  data class JsonParseError(override val msg: String, val jsonString: String?) : NotaryToolError

  data class GeneralError(override val msg: String) : NotaryToolError


}





