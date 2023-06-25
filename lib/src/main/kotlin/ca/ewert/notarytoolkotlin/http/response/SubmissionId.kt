package ca.ewert.notarytoolkotlin.http.response

import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result



/**
 * Wrapper around the Submission Id, used with Apple's Notary API.
 * The identifier that you receive from the notary service when you post to
 * Submit Software to start a new submission. Value must match:
 * `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`
 *
 * @property submissionId
 * @author Victor Ewert
 */
data class SubmissionId internal constructor(val submissionId: String) {

  companion object {

    /**
     * Regex used to validate the submissionId
     */
    private val VALIDATION_REGEX: Regex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    /**
     * Validates and then creates an a [SubmissionId]. To be valid, the String must match:
     * `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`
     *
     * This should only be used when receiving an submissionId String from an external source,
     * in general it is better use the [SubmissionId] from a [SubmissionInfo]
     *
     * @param submissionIdString A valid submission id from a previous submission to the Notary API
     * @return A [SubmissionId] or a [NotaryToolError.MalformedSubmissionIdError]
     */
    @JvmStatic
    fun of(submissionIdString: String): Result<SubmissionId, NotaryToolError.MalformedSubmissionIdError> {
      return if (submissionIdString.matches(VALIDATION_REGEX)) {
        Ok(SubmissionId(submissionIdString))
      } else {
        Err(NotaryToolError.MalformedSubmissionIdError("String passed in is not valid", submissionIdString))
      }
    }
  }
}