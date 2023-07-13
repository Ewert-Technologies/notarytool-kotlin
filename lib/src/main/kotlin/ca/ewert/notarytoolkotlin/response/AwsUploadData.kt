package ca.ewert.notarytoolkotlin.response

import ca.ewert.notarytoolkotlin.NotaryToolClient

/**
 * Information about a successful upload of the software file
 * to the AWS S3 Bucket.
 *
 * @property eTag The eTag received from AWS.
 * @property submissionId The submission id received from Apple Notarization API.
 * A unique identifier for this submission. Use this value to track the status of your submission.
 * For example, you use it as the submissionID parameter in the [NotaryToolClient.getSubmissionStatus] call,
 * or to match against the id field in the response from the [NotaryToolClient.getPreviousSubmissions] call.
 * @author Victor Ewert
 */
data class AwsUploadData(val eTag: String?, val submissionId: SubmissionId)
