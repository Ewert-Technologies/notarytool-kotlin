package ca.ewert.notarytoolkotlin.response

import ca.ewert.notarytoolkotlin.errors.NotaryToolError.UserInputError.MalformedSubmissionIdError
import ca.ewert.notarytoolkotlin.i18n.ErrorStringsResource
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

/**
 * Wrapper around the `submissionId`, used by Apple's Notary API.
 * The identifier that you receive from the notary service when you post to
 * Submit Software to start a new submission. Value must match:
 * `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`
 *
 * @property id
 * @author Victor Ewert
 */
data class SubmissionId internal constructor(val id: String) {

  companion object {

    /**
     * Regex used to validate the submissionId
     */
    @JvmStatic
    private val VALIDATION_REGEX: Regex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    /**
     * Validates and then creates an a [SubmissionId]. To be valid, the String must match:
     * `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`
     *
     * This should only be used when receiving a submissionId as a String from an external source.
     * In general, it is better use the [SubmissionId] from a [SubmissionInfo] for subsequent uses.
     *
     * @param submissionIdString A valid submission id from a previous submission to the Notary API
     * @return A [SubmissionId] or a [NotaryToolError.MalformedSubmissionIdError]
     */
    @JvmStatic
    fun of(submissionIdString: String): Result<SubmissionId, MalformedSubmissionIdError> {
      return if (submissionIdString.matches(VALIDATION_REGEX)) {
        Ok(SubmissionId(submissionIdString))
      } else {
        Err(
          MalformedSubmissionIdError(
            msg = ErrorStringsResource.getString("submission.id.invalid.string.error"),
            invalidId = submissionIdString,
          ),
        )
      }
    }
  }

  /**
   * Returns a String representation of the object.
   *
   * @return The submission id
   */
  override fun toString(): String {
    return id
  }
}
