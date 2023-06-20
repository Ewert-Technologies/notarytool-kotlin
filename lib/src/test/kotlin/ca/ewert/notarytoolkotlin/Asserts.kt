package ca.ewert.notarytoolkotlin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import java.time.Duration
import java.time.Instant
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

/**
 * Asserts that a [Instant] is closes to the expected [Instant] within the tolerance specified.
 */
fun Assert<Instant>.isCloseTo(expected: Instant, tolerance: Duration) = given { actual ->
  val lowerBound = expected.minus(tolerance)
  val upperBound = expected.plus(tolerance)
  if (actual.isBefore(upperBound) && actual.isAfter(lowerBound)) return
  expected("ZonedDateTime: ${show(actual)} but was expected to be between ${show(lowerBound)} and ${show(upperBound)}")
}

fun Assert<Either<Any, Any>>.isRight() = given { actual ->
  if (actual.isRight()) return
  expected("$actual to be ${actual.right()}")
}

fun Assert<Either<Any, Any>>.isLeft() = given { actual ->
  if (actual.isLeft()) return
  expected("$actual to be ${actual.left()}")
}