package ca.ewert.notarytoolkotlin.errors

import java.security.interfaces.ECPrivateKey

/**
 * Top-level parent of all notarytool-kotlin Error
 *
 * @author Victor Ewert
 */
sealed interface NotaryToolError {

  /** Error Message */
  val msg: String

  /**
   * Top-level parent of all Errors related to input received from the user.
   *
   * These errors could be caused by errors creating the JsonWebToken (i.e. invalid issuerId, private key id,
   * or private key) an Authentication Issue, or an invalid submission id.
   *
   * @author Victor Ewert
   */
  sealed interface UserInputError : NotaryToolError {

    /**
     * An error caused by a submissionId that has an incorrect format. According to the App Store Connect API,
     * the submissionId must match: `/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/`
     *
     * @property msg Error message details
     * @property invalidId The invalid id that was used
     * @author Victor Ewert
     */
    data class MalformedSubmissionIdError(override val msg: String, val invalidId: String) : UserInputError

    /**
     * An error caused when the submissionId used in a request is incorrect, and could not be found.
     *
     * Internally this is trapped as an HTTP 404 error from the App Store Connect API Web Service.
     *
     * @property msg Error message details.
     * @author Victor Ewert
     */
    data class InvalidSubmissionIdError(
      override val msg: String,
    ) : UserInputError


    /**
     * Top-level parent of all Errors related to creating or using the Json Web Token.
     * These error are most likely caused by an invalid issuerId, private key id,
     * private key file, or an expired web token.
     *
     * @author Victor Ewert
     */
    sealed interface JsonWebTokenError : UserInputError {

      /**
       * An error caused when the Private Key (`.p8`) file cannot be found
       *
       * @property msg Error message details.
       * @author Victor Ewert
       */
      data class PrivateKeyNotFoundError(override val msg: String) : JsonWebTokenError

      /**
       * An error caused when there is an Exception generating the [ECPrivateKey] (used to sign the Json Web Token)
       * from the private key file provided.
       *
       * @property msg Error message including any Exception message.
       * @author Victor Ewert
       */
      data class InvalidPrivateKeyError(override val msg: String) : JsonWebTokenError

      /**
       * Error for when there is a problem or Exception when generating or signing
       * the Json Web Token.
       *
       * @property msg Error message including an Exception message
       * @author Victor Ewert
       */
      data class TokenCreationError(override val msg: String) : JsonWebTokenError

      /**
       * An error caused when the request to the App Store Connect API Web Service fails the
       * authentication check. This is typically caused by an invalid issuerId, private key id,
       * private key file, or an expired web token.
       *
       * Internally this is trapped as an HTTP 401 error from the App Store Connect API Web Service.
       *
       * @author Victor Ewert
       */
      data class AuthenticationError(override val msg: String) : UserInputError
    }
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
