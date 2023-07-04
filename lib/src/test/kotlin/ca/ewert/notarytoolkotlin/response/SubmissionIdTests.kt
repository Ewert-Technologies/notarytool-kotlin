package ca.ewert.notarytoolkotlin.response

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import ca.ewert.notarytoolkotlin.NotaryToolError
import ca.ewert.notarytoolkotlin.isErrAnd
import ca.ewert.notarytoolkotlin.isOkAnd
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit Tests for [SubmissionId]
 *
 * @author Victor Ewert
 */
class SubmissionIdTests {

  /**
   * Tests creating a [SubmissionId] using valid data
   */
  @Test
  @DisplayName("Valid - Test")
  fun validTest() {
    val submissionIdResult =
      SubmissionId.of("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    assertThat(submissionIdResult).isOkAnd().prop(SubmissionId::id)
      .isEqualTo("2efe2717-52ef-43a5-96dc-0797e4ca1041")
  }

  /**
   * Test attempting to create a [SubmissionId] using invalid data.
   * Test short String
   */
  @Test
  @DisplayName("Invalid Test - 1")
  fun invalidTest1() {
    val submissionIdResult =
      SubmissionId.of("Hello World")
    assertThat(submissionIdResult).isErrAnd().prop(NotaryToolError.UserInputError.MalformedSubmissionIdError::invalidId)
      .isEqualTo("Hello World")
  }

  /**
   * Test attempting to create a [SubmissionId] using invalid data.
   * Correct length and pattern, but invalid characters
   */
  @Test
  @DisplayName("Invalid Test - 2")
  fun invalidTest2() {
    val submissionIdResult =
      SubmissionId.of("2efe2717-52ef-43a5-96dc-0797e4ve1041")
    assertThat(submissionIdResult).isErrAnd().prop(NotaryToolError.UserInputError.MalformedSubmissionIdError::invalidId)
      .isEqualTo("2efe2717-52ef-43a5-96dc-0797e4ve1041")
  }
}
