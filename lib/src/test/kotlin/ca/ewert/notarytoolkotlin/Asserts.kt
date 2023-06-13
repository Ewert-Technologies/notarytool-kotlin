package ca.ewert.notarytoolkotlin

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Some custom AssertK asserts
 *
 * Created: 2023-06-09
 * @author vewert
 */

/**
 * Asserts that a [ZonedDateTime] is closes to the expected [ZonedDateTime] within the tolerance specified.
 */
fun Assert<ZonedDateTime>.isCloseTo(expected: ZonedDateTime, tolerance: Duration) = given { actual ->
  val lowerBound: ZonedDateTime = expected.minus(tolerance)
  val upperBound: ZonedDateTime = expected.plus(tolerance)
  if (actual.isBefore(upperBound) && actual.isAfter(lowerBound)) return
  expected("ZonedDateTime: ${show(actual)} but was expected to be between ${show(lowerBound)} and ${show(upperBound)}")
}