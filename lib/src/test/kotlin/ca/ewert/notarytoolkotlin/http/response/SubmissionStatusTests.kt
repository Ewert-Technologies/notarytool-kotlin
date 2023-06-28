package ca.ewert.notarytoolkotlin.http.response

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

/**
 * Unit Tests for [SubmissionStatus]
 *
 * Created: 2023-06-16
 * @author vewert
 */
class SubmissionStatusTests {

  /**
   * Tests [SubmissionStatus.ACCEPTED]. Tests creating from lowercase, uppercase and invalid String
   */
  @Test
  fun createAcceptedTest() {
    val submissionStatus1 = SubmissionStatus.fromString("Accepted")
    assertThat(submissionStatus1).isEqualTo(SubmissionStatus.ACCEPTED)
    assertThat(submissionStatus1.displayName).isEqualTo("Accepted")
    assertThat(submissionStatus1.toString()).isEqualTo("Accepted")

    val submissionStatus2 = SubmissionStatus.fromString("AccepteD")
    assertThat(submissionStatus2).isEqualTo(SubmissionStatus.ACCEPTED)

    val submissionStatus3 = SubmissionStatus.fromString("acccepted")
    assertThat(submissionStatus3).isEqualTo(SubmissionStatus.UNKNOWN)
    assertThat(submissionStatus3.displayName).isEqualTo("Unknown")
    assertThat(submissionStatus3.toString()).isEqualTo("Unknown")
  }

  /**
   * Tests [SubmissionStatus.IN_PROGRESS]. Tests creating from lowercase, uppercase and invalid String,
   * also checks that "In-Progress" (case-insensitive) is allowed
   */
  @Test
  fun createInProgressTest() {
    val submissionStatus1 = SubmissionStatus.fromString("In Progress")
    assertThat(submissionStatus1).isEqualTo(SubmissionStatus.IN_PROGRESS)
    assertThat(submissionStatus1.displayName).isEqualTo("In Progress")
    assertThat(submissionStatus1.toString()).isEqualTo("In Progress")

    val submissionStatus2 = SubmissionStatus.fromString("in PROGRESS")
    assertThat(submissionStatus2).isEqualTo(SubmissionStatus.IN_PROGRESS)

    val submissionStatus3 = SubmissionStatus.fromString("In-Progress")
    assertThat(submissionStatus3).isEqualTo(SubmissionStatus.IN_PROGRESS)

    val submissionStatus4 = SubmissionStatus.fromString("IN-PROGRESS")
    assertThat(submissionStatus4).isEqualTo(SubmissionStatus.IN_PROGRESS)

    val submissionStatus5 = SubmissionStatus.fromString("in progesss")
    assertThat(submissionStatus5).isEqualTo(SubmissionStatus.UNKNOWN)
    assertThat(submissionStatus5.displayName).isEqualTo("Unknown")
    assertThat(submissionStatus5.toString()).isEqualTo("Unknown")
  }

  /**
   * Tests [SubmissionStatus.INVALID]. Tests creating from lowercase, uppercase and invalid String
   */
  @Test
  fun createInvalidTest() {
    val submissionStatus1 = SubmissionStatus.fromString("Invalid")
    assertThat(submissionStatus1).isEqualTo(SubmissionStatus.INVALID)
    assertThat(submissionStatus1.displayName).isEqualTo("Invalid")
    assertThat(submissionStatus1.toString()).isEqualTo("Invalid")

    val submissionStatus2 = SubmissionStatus.fromString("INValid")
    assertThat(submissionStatus2).isEqualTo(SubmissionStatus.INVALID)

    val submissionStatus3 = SubmissionStatus.fromString("Invalide")
    assertThat(submissionStatus3).isEqualTo(SubmissionStatus.UNKNOWN)
    assertThat(submissionStatus3.displayName).isEqualTo("Unknown")
    assertThat(submissionStatus3.toString()).isEqualTo("Unknown")
  }

  /**
   * Tests [SubmissionStatus.REJECTED]. Tests creating from lowercase, uppercase and invalid String
   */
  @Test
  fun createRejectedTest() {
    val submissionStatus1 = SubmissionStatus.fromString("Rejected")
    assertThat(submissionStatus1).isEqualTo(SubmissionStatus.REJECTED)
    assertThat(submissionStatus1.displayName).isEqualTo("Rejected")
    assertThat(submissionStatus1.toString()).isEqualTo("Rejected")

    val submissionStatus2 = SubmissionStatus.fromString("REjected")
    assertThat(submissionStatus2).isEqualTo(SubmissionStatus.REJECTED)

    val submissionStatus3 = SubmissionStatus.fromString("Injected")
    assertThat(submissionStatus3).isEqualTo(SubmissionStatus.UNKNOWN)
    assertThat(submissionStatus3.displayName).isEqualTo("Unknown")
    assertThat(submissionStatus3.toString()).isEqualTo("Unknown")
  }
}
