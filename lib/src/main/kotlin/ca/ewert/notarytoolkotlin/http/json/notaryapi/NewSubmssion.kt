package ca.ewert.notarytoolkotlin.http.json.notaryapi

/**
 * Top level Request for making a **`Submit Software`** Request.
 * Corresponds to [`NewSubmissionRequest`](https://developer.apple.com/documentation/notaryapi/newsubmissionrequest),
 * from Apple's API. _Data that you provide when starting a submission to the notary service._
 *
 * @property notifications `NewSubmissionRequest.Notifications` An optional array of notifications that
 * you want to receive when notarization finishes. Omit this key if you don’t need a notification.
 * @property sha256 (Required) A cryptographic hash of the software that you want to notarize,
 * computed using Secure Hashing Algorithm 2 (SHA-2) with a 256-bit digest.
 * Supply the hash as a string of 64 hexadecimal digits. You must compute the hash from the
 * exact version of the software that you plan to upload to Amazon S3. Value: `/[A-Fa-f0-9]{64/`
 * @property submissionName (Required) The name of the file that you plan to submit.
 * The service includes this name in its responses when you ask for the status of a submission,
 * get a list of previous submissions, or get a log file corresponding to a submission.
 * The file name doesn’t have to be unique among all your submissions, but making it so might help
 * you to distinguish among submissions in service responses.
 */
data class NewSubmissionRequestJson(
  val notifications: List<NotificationJson>,
  val sha256: String,
  val submissionName: String
)

/**
 * Corresponds to [`NewSubmissionRequest.Notifications`](https://developer.apple.com/documentation/notaryapi/newsubmissionrequest/notifications),
 * from Apples API. _A notification that the notary service sends you when notarization finishes._
 *
 * @property channel The channel that the service uses to notify you when notarization completes. The only supported value for this key is webhook.
 * @property target The URL that the notary service accesses when notarization completes.
 */
data class NotificationJson(
  val channel: String,
  val target: String
)