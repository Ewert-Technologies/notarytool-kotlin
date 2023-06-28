package ca.ewert.notarytoolkotlin.errors

/**
 * Top-level parent of all notarytool-kotlin Erro
 *
 * @author Victor Ewert
 */
sealed interface NotaryToolError {

  /** Error Message */
  val msg: String

  sealed interface UserInputError : NotaryToolError {
    data class MalformedSubmissionIdError(override val msg: String, val invalidId: String) : UserInputError

    data class InvalidSubmissionId(
      override val msg: String,
    ) : UserInputError

    /**
     * Parent of all Errors related to working with Json Web Token
     */
    sealed interface JsonWebTokenError : UserInputError {

      /**
       * Error for when the Private Key (`.p8`) file cannot be found
       */
      data class PrivateKeyNotFoundError(override val msg: String) : JsonWebTokenError

      data class InvalidPrivateKeyError(override val msg: String) : JsonWebTokenError

      /**
       * Error for when there is a problem generating the Json Web Token.
       * The [msg] contains details about the error.
       */
      data class TokenCreationError(override val msg: String) : JsonWebTokenError
    }

    data class AuthenticationError(override val msg: String) : UserInputError
  }

  sealed interface HttpError : NotaryToolError {
    val httpStatusCode: Int
    val httpStatusMsg: String
    val requestUrl: String
    val contentBody: String?

    data class ClientError4xx(
      override val msg: String,
      override val httpStatusCode: Int,
      override val httpStatusMsg: String,
      override val requestUrl: String = "",
      override val contentBody: String?,
    ) : HttpError

    data class ServerError5xx(
      override val msg: String,
      override val httpStatusCode: Int,
      override val httpStatusMsg: String,
      override val requestUrl: String = "",
      override val contentBody: String?,
    ) : HttpError

    data class OtherError(
      override val msg: String,
      override val httpStatusCode: Int,
      override val httpStatusMsg: String,
      override val requestUrl: String = "",
      override val contentBody: String?,
    ) : HttpError
  }

  /**
   * @property msg The error message
   * @property jsonString The json String that was used when attempting to parse
   */
  data class JsonParseError(override val msg: String, val jsonString: String?) : NotaryToolError

  data class GeneralError(override val msg: String) : NotaryToolError
}