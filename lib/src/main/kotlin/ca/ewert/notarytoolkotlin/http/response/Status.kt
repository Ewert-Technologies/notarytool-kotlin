package ca.ewert.notarytoolkotlin.http.response

/**
 * Various status states of a Submission
 *
 * @property displayName The display name of the status
 * @author Victor Ewert
 */
enum class Status(val displayName: String) {

  /**
   * A Submission status of `Accepted`
   */
  ACCEPTED("Accepted"),

  /**
   * A Submission status of `In Progress`
   */
  IN_PROGRESS("In Progress"),

  /**
   * A Submission status of `Invalid`
   */
  INVALID("Invalid"),

  /**
   * A Submission status of `Rejected`
   */
  REJECTED("Rejected"),

  /**
   * When the submission status returned from the API doesn't match
   * a known status
   */
  UNKNOWN("Unknown"),
  ;

  /**
   * Returns a String representation of the object.
   *
   * @return The display name.
   */
  override fun toString(): String {
    return displayName
  }

  companion object {

    /**
     * Return the enum value corresponding to the String passed in. If the String
     * doesn't match then [UNKNOWN] is returned.
     *
     * @param stringValue The String value to convert
     */
    fun fromString(stringValue: String): Status {
      return when {
        stringValue.equals(ACCEPTED.displayName, true) -> ACCEPTED
        (stringValue.equals(IN_PROGRESS.displayName, true)) ||
          (stringValue.equals("In-Progress", true)) -> IN_PROGRESS

        stringValue.equals(INVALID.displayName, true) -> INVALID
        stringValue.equals(REJECTED.displayName, true) -> REJECTED
        else -> UNKNOWN
      }
    }
  }
}
