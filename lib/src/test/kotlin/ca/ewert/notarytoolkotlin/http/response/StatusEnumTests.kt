package ca.ewert.notarytoolkotlin.http.response

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

/**
 * Unit Tests for the [Status] enum.
 *
 * Created: 2023-06-16
 * @author vewert
 */
class StatusEnumTests {

  /**
   * Tests [Status.ACCEPTED]. Tests creating from lowercase, uppercase and invalid String
   */
  @Test
  fun createAcceptedTest() {
    val status1 = Status.fromString("Accepted")
    assertThat(status1).isEqualTo(Status.ACCEPTED)
    assertThat(status1.displayName).isEqualTo("Accepted")
    assertThat(status1.toString()).isEqualTo("Accepted")

    val status2 = Status.fromString("AccepteD")
    assertThat(status2).isEqualTo(Status.ACCEPTED)

    val status3 = Status.fromString("acccepted")
    assertThat(status3).isEqualTo(Status.UNKNOWN)
    assertThat(status3.displayName).isEqualTo("Unknown")
    assertThat(status3.toString()).isEqualTo("Unknown")
  }

  /**
   * Tests [Status.IN_PROGRESS]. Tests creating from lowercase, uppercase and invalid String,
   * also checks that "In-Progress" (case-insensitive) is allowed
   */
  @Test
  fun createInProgressTest() {
    val status1 = Status.fromString("In Progress")
    assertThat(status1).isEqualTo(Status.IN_PROGRESS)
    assertThat(status1.displayName).isEqualTo("In Progress")
    assertThat(status1.toString()).isEqualTo("In Progress")

    val status2 = Status.fromString("in PROGRESS")
    assertThat(status2).isEqualTo(Status.IN_PROGRESS)

    val status3 = Status.fromString("In-Progress")
    assertThat(status3).isEqualTo(Status.IN_PROGRESS)

    val status4 = Status.fromString("IN-PROGRESS")
    assertThat(status4).isEqualTo(Status.IN_PROGRESS)

    val status5 = Status.fromString("in progesss")
    assertThat(status5).isEqualTo(Status.UNKNOWN)
    assertThat(status5.displayName).isEqualTo("Unknown")
    assertThat(status5.toString()).isEqualTo("Unknown")
  }

  /**
   * Tests [Status.INVALID]. Tests creating from lowercase, uppercase and invalid String
   */
  @Test
  fun createInvalidTest() {
    val status1 = Status.fromString("Invalid")
    assertThat(status1).isEqualTo(Status.INVALID)
    assertThat(status1.displayName).isEqualTo("Invalid")
    assertThat(status1.toString()).isEqualTo("Invalid")

    val status2 = Status.fromString("INValid")
    assertThat(status2).isEqualTo(Status.INVALID)

    val status3 = Status.fromString("Invalide")
    assertThat(status3).isEqualTo(Status.UNKNOWN)
    assertThat(status3.displayName).isEqualTo("Unknown")
    assertThat(status3.toString()).isEqualTo("Unknown")
  }

  /**
   * Tests [Status.REJECTED]. Tests creating from lowercase, uppercase and invalid String
   */
  @Test
  fun createRejectedTest() {
    val status1 = Status.fromString("Rejected")
    assertThat(status1).isEqualTo(Status.REJECTED)
    assertThat(status1.displayName).isEqualTo("Rejected")
    assertThat(status1.toString()).isEqualTo("Rejected")

    val status2 = Status.fromString("REjected")
    assertThat(status2).isEqualTo(Status.REJECTED)

    val status3 = Status.fromString("Injected")
    assertThat(status3).isEqualTo(Status.UNKNOWN)
    assertThat(status3.displayName).isEqualTo("Unknown")
    assertThat(status3.toString()).isEqualTo("Unknown")
  }
}
