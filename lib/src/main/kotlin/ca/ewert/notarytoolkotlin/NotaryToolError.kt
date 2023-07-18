package ca.ewert.notarytoolkotlin

import ca.ewert.notarytoolkotlin.response.ResponseMetaData
import java.security.interfaces.ECPrivateKey

/**
 * Top-level parent of all notarytool-kotlin errors
 *
 * @author Victor Ewert
 */
sealed interface NotaryToolError {

  /** The error message. */
  val msg: String

  /**
   * Top-level parent of all errors related to credentials received from the end-user, i.e. the user attempting
   * to notarize the software.
   *
   * These errors could be caused by errors creating the JsonWebToken (i.e. invalid or malformed issuer id,
   * private key id, or private key) an authentication Issue (i.e incorrect issuer id, private key id, or private key),
   * or by an invalid submission id.
   *
   * Errors of these types should be trapped and reported back to the end-user for corrective action.
   *
   * @author Victor Ewert
   */
  sealed interface UserInputError : NotaryToolError {

    /**
     * An error caused by a submissionId that has an incorrect format. According to the Notary API,
     * the submissionId must match: `"/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/"`
     *
     * @property msg Error message details
     * @property invalidId The invalid id that was used
     * @author Victor Ewert
     */
    data class MalformedSubmissionIdError(override val msg: String, val invalidId: String) : UserInputError

    /**
     * An error caused when the submissionId used in a request is incorrect, and could not be found.
     *
     * Internally this is trapped as an HTTP 404 error from the Notary API Web Service, but is reported here
     * as a [UserInputError], since the submission id passed in is incorrect.
     *
     * @property msg Error message details.
     * @author Victor Ewert
     */
    data class InvalidSubmissionIdError(override val msg: String) : UserInputError

    /**
     * An error caused when the request to the Notary API Web Service fails the
     * authentication check. This is typically caused by an incorrect issuer id, private key id,
     * private key file, or an expired web token.
     *
     * Internally this is trapped as an HTTP 401 error from the Notary API Web Service, but is reported here
     * as a [UserInputError], since it is like caused by invalid user credentials.
     *
     * @author Victor Ewert
     */
    data class AuthenticationError(override val msg: String) : UserInputError

    /**
     * Top-level parent of all errors related to creating or using the Json Web Token.
     * These error are most likely caused by an invalid issuer id, private key id,
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
       * An error for when there is a problem or Exception when generating or signing
       * the Json Web Token.
       *
       * @property msg Error message including any Exception message.
       * @author Victor Ewert
       */
      data class TokenCreationError(override val msg: String) : JsonWebTokenError
    }
  }

  /**
   * Top-level parent of HTTP Errors received from the Notary API Web Service.
   *
   * @property responseMetaData The response metadata, may be useful in debugging the error.
   * @author Victor Ewert
   */
  sealed interface HttpError : NotaryToolError {
    val responseMetaData: ResponseMetaData

    /**
     * A Client Error response, in the range of 400 to 499: *"The request contains bad syntax
     * or cannot be fulfilled."*
     *
     * @author Victor Ewert
     */
    data class ClientError4xx(
      override val msg: String,
      override val responseMetaData: ResponseMetaData,
    ) : HttpError

    /**
     * A Server Error response, in the range of 500 to 599: *"The server failed to fulfil
     * an apparently valid request."*
     *
     * @author Victor Ewert
     */
    data class ServerError5xx(
      override val msg: String,
      override val responseMetaData: ResponseMetaData,
    ) : HttpError

    /**
     * Any other HTTP Error.
     *
     * @author Victor Ewert
     */
    data class OtherError(
      override val msg: String,
      override val responseMetaData: ResponseMetaData,
    ) : HttpError
  }

  /**
   * An error caused when there is a problem parsing (deserializing) the json content sent by
   * the Notary API Web Service. This should only occur if the Notary API
   * changes.
   *
   * @property msg The error message.
   * @property jsonString The json String that was used deserializing, if available.
   * @author Victor Ewert
   */
  data class JsonParseError(override val msg: String, val jsonString: String?) : NotaryToolError

  /**
   * An error caused when there is a problem serializing a data object into a json string.
   *
   * @property msg The error message.
   * @property dataObject A String representation of the data object that was used
   * when serializing into the json String.
   * @author Victor Ewert
   */
  data class JsonCreateError(override val msg: String, val dataObject: String) : NotaryToolError

  /**
   * An error caused when there is a problem retrieving the submission log for a specific software submission.
   * This could be caused, for example, by an invalid `developerLogUrl` returned by the
   * Notary API, or by an HTTP error while attempting to download the log.
   *
   * @property msg The error message.
   * @author Victor Ewert
   */
  data class SubmissionLogError(override val msg: String) : NotaryToolError

  /**
   * An error caused when there is a connection issue. Examples: no internet connection, the connection
   * timed out, the connection dropped, etc.
   *
   * @property msg The connection error message.
   */
  data class ConnectionError(override val msg: String) : NotaryToolError

  /**
   * An error that can occur when uploading the software submission to the
   * Amazon S3 Server.
   *
   * @property msg The error message.
   * @property exception The exception that occurred. Can be used to access the specific error message.
   */
  data class AwsUploadError(override val msg: String, val exception: Exception) : NotaryToolError

  /**
   * Not an error, but an indication that polling has reached the maximum
   * number of attempts.
   */
  data class PollingTimeout(override val msg: String) : NotaryToolError

  /**
   * Any other error.
   *
   * @author Victor Ewert
   */
  data class GeneralError(override val msg: String) : NotaryToolError
}
