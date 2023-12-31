package ca.ewert.notarytoolkotlin.response

import java.time.Instant

/**
 * Information about the status of a submission.
 *
 * @property createdDate The date that the submission process was started. May be `null`
 * if the `createdDate` value returned by the Notary API can't be parsed.
 * In this case, use [createdDateText] and attempt to parse it manually.
 * @property createdDateText The date that the submission process was started, as a Text String, as returned by the Notary API.
 * @property name The name that was specified in the submissionName field of the
 * Submit Software call when the submission was started, i.e. the name of the software file
 * that was submitted.
 * @property status The status of the submission.
 * @property statusText The status as text, as returned by the Notary API.
 * @property id The unique identifier for this submission.
 * @author Victor Ewert
 */
data class SubmissionInfo internal constructor(
  val createdDate: Instant?,
  val createdDateText: String,
  val name: String,
  val status: Status,
  val statusText: String,
  val id: SubmissionId,
)
