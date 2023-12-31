package ca.ewert.notarytoolkotlin.response

import ca.ewert.notarytoolkotlin.json.notaryapi.NewSubmissionResponseJson

/**
 * Response from sending a request the [Submit Software](https://developer.apple.com/documentation/notaryapi/submit_software)
 * Endpoint.
 *
 * Contains credentials, and other information, required to upload your software to the Amazon S3 Server.
 *
 * @author Victor Ewert
 */
class NewSubmissionResponse internal constructor(
  responseMetaData: ResponseMetaData,
  jsonResponse: NewSubmissionResponseJson,
) : NotaryApiResponse(responseMetaData) {

  /** The unique identifier for this submission. */
  val id: SubmissionId

  /** An access key that you use in a call to Amazon S3, to upload the software. */
  val awsAccessKeyId: String

  /** A secret key that you use in a call to Amazon S3, to upload the software. */
  val awsSecretAccessKey: String

  /** A session token that you use in a call to Amazon S3, to upload the software. */
  val awsSessionToken: String

  /** The Amazon S3 bucket that you upload your software into. */
  val bucket: String

  /** The object key that identifies your software upload within the bucket. */
  val objectKey: String

  init {
    id = SubmissionId(jsonResponse.newSubmissionResponseData.id)
    awsAccessKeyId = jsonResponse.newSubmissionResponseData.attributes.awsAccessKeyId
    awsSecretAccessKey = jsonResponse.newSubmissionResponseData.attributes.awsSecretAccessKey
    awsSessionToken = jsonResponse.newSubmissionResponseData.attributes.awsSessionToken
    bucket = jsonResponse.newSubmissionResponseData.attributes.bucket
    objectKey = jsonResponse.newSubmissionResponseData.attributes.objectKey
  }

  /**
   * Returns a String representation of the object, suitable for debugging.
   */
  override fun toString(): String {
    return "NewSubmissionResponse(" +
      "id=$id, " +
      "awsAccessKeyId='$awsAccessKeyId', " +
      "awsSecretAccessKey='$awsSecretAccessKey', " +
      "awsSessionToken='$awsSessionToken', " +
      "bucket='$bucket', " +
      "objectKey='$objectKey'" +
      ")"
  }
}
