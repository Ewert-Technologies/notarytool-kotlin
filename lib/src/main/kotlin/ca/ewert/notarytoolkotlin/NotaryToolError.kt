package ca.ewert.notarytoolkotlin

import ca.ewert.notarytoolkotlin.response.ResponseMetaData
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

  /**
   * Top-level parent of HTTP Errors received from the App Store Connect API Web Service
   *
   * @property httpStatusCode The HTTP status code.
   * @property httpStatusMsg The HTTP status message.
   * @property requestUrl The URL used in the request.
   * @property contentBody The body of the request.
   * @author Victor Ewert
   */
  sealed interface HttpError : NotaryToolError {
    @Deprecated("Use responseMetaData instead")
    val httpStatusCode: Int

    @Deprecated("Use responseMetaData instead")
    val httpStatusMsg: String
    val requestUrl: String

    @Deprecated("Use responseMetaData instead")
    val contentBody: String?
    val responseMetaData: ResponseMetaData

    /**
     * A Client Error response, in the range of 400 to 499. *"The request contains bad syntax
     * or cannot be fulfilled."*
     *
     * @author Victor Ewert
     */
    data class ClientError4xx(
      override val msg: String,
      @Deprecated("Use responseMetaData instead")
      override val httpStatusCode: Int = -1,
      @Deprecated("Use responseMetaData instead")
      override val httpStatusMsg: String = "",
      override val requestUrl: String = "",
      @Deprecated("Use responseMetaData instead")
      override val contentBody: String? = "",
      override val responseMetaData: ResponseMetaData,
    ) : HttpError

    /**
     * A Server Error response, in the range of 500 to 599. *"The server failed to fulfil
     * an apparently valid request."*
     *
     * @author Victor Ewert
     */
    data class ServerError5xx(
      override val msg: String,
      @Deprecated("Use responseMetaData instead")
      override val httpStatusCode: Int = -1,
      @Deprecated("Use responseMetaData instead")
      override val httpStatusMsg: String = "",
      override val requestUrl: String = "",
      @Deprecated("Use responseMetaData instead")
      override val contentBody: String? = "",
      override val responseMetaData: ResponseMetaData,
    ) : HttpError

    /**
     * Any other HTTP Error.
     *
     * @author Victor Ewert
     */
    data class OtherError(
      override val msg: String,
      @Deprecated("Use responseMetaData instead")
      override val httpStatusCode: Int = -1,
      @Deprecated("Use responseMetaData instead")
      override val httpStatusMsg: String = "",
      override val requestUrl: String = "",
      @Deprecated("Use responseMetaData instead")
      override val contentBody: String? = "",
      override val responseMetaData: ResponseMetaData,
    ) : HttpError
  }

  /**
   * An error caused when there is a problem parsing the json content sent by
   * the App Store Connect API Web Service. This should only occur if the Web API
   * changes.
   *
   * @property msg The error message
   * @property jsonString The json String that was used when attempting to parse
   * @author Victor Ewert
   */
  data class JsonParseError(override val msg: String, val jsonString: String?) : NotaryToolError

  /**
   * An error caused when there is a problem generating a json String from a data
   * object.
   *
   * @property msg The error message.
   * @property dataObject A String representation of the data object that was used
   * to create the json String.
   * @author Victor Ewert
   */
  data class JsonCreateError(override val msg: String, val dataObject: String) : NotaryToolError

  /**
   * An error caused when there is a problem retrieving the submission log.
   * This could be caused, for example, by an invalid `developerLogUrl` returned by the
   * Notary API, or by an http error while attempting to download the log.
   *
   * @property msg The error message
   * @author Victor Ewert
   */
  data class SubmissionLogError(override val msg: String) : NotaryToolError

  /**
   * An error caused when there is a connection issue.
   *
   * @property msg The connection error message
   */
  data class ConnectionError(override val msg: String) : NotaryToolError

  /**
   * An error that can occur when uploading the software submission to the
   * AWS Servers.
   *
   * @property msg The error message
   * @property exception The exception that occurred. Can be used to access the specific error message.
   */
  data class AwsUploadError(override val msg: String, val exception: Exception) : NotaryToolError

  /**
   * Any other error.
   *
   * @author Victor Ewert
   */
  data class GeneralError(override val msg: String) : NotaryToolError
}
