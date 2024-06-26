package ca.ewert.notarytoolkotlin

import assertk.Assert
import assertk.assertions.containsMatch
import assertk.assertions.isEqualTo
import assertk.assertions.support.appendName
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.github.michaelbull.result.Result
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Some custom AssertK asserts
 *
 * Created: 2023-06-09
 * @author vewer
 */

/**
 * Asserts that a [ZonedDateTime] is closes to the expected [ZonedDateTime] within the tolerance specified.
 */
fun Assert<ZonedDateTime>.isCloseTo(expected: ZonedDateTime, tolerance: Duration) = given { actual ->
  val lowerBound: ZonedDateTime = expected.minus(tolerance)
  val upperBound: ZonedDateTime = expected.plus(tolerance)
  if (actual.isBefore(upperBound) && actual.isAfter(lowerBound)) return
  expected("${show(actual)} to be between ${show(lowerBound)} and ${show(upperBound)}")
}

/**
 * Asserts that a [Instant] is close to the expected [Instant] within the tolerance specified.
 */
fun Assert<Instant>.isCloseTo(expected: Instant, tolerance: Duration) = given { actual ->
  val lowerBound = expected.minus(tolerance)
  val upperBound = expected.plus(tolerance)
  if (actual.isBefore(upperBound) && actual.isAfter(lowerBound)) return
  expected("${show(actual)} to be between ${show(lowerBound)} and ${show(upperBound)}")
}

/**
 * Asserts that a [Result] is Ok
 */
fun <V, E> Assert<Result<V, E>>.isOk() = given { actual ->
  if (!actual.isOk) {
    expected("$actual to be Result.Ok}")
  }
}

/**
 * Asserts that a [Result] is Ok, and then chains the Ok result
 * so further asserts can be done on the Ok Value
 */
fun <V, E> Assert<Result<V, E>>.isOkAnd(): Assert<V> = transform(appendName("Result.Ok value", ".")) { actual ->
  if (actual.isOk) {
    actual.value
  } else {
    expected("${show(actual)} to be Result.Ok}")
  }
}

/**
 * Asserts that a [Result] is an Err
 */
fun <V, E> Assert<Result<V, E>>.isErr() = given { actual ->
  if (!actual.isErr) {
    expected("${show(actual)} to be Result.Err")
  }
}

/**
 * Asserts that a [Result] is an Err and then chains the Err result
 * so further asserts can be done on the Err Value
 */
fun <V, E> Assert<Result<V, E>>.isErrAnd() = transform { actual ->
  if (actual.isErr) {
    actual.error
  } else {
    expected("${show(actual)} to be Result.Err")
  }
}

/**
 * Checks the message of a
 */
fun Assert<NotaryToolError>.hasMessage(expected: String) = given { actual ->
  assertk.assertThat(actual.msg).isEqualTo(expected)
}

/**
 * Checks the message of a [NotaryToolError] using Regex.
 */
fun Assert<NotaryToolError>.messageContainsMatch(regex: Regex) = given { actual ->
  assertk.assertThat(actual.msg).containsMatch(regex)
}
