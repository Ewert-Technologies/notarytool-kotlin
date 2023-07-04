package ca.ewert.notarytoolkotlin.json.notaryapi

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import ca.ewert.notarytoolkotlin.isErr
import ca.ewert.notarytoolkotlin.isOk
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [ErrorResponseJson]
 *
 * @author Victor Ewert
 */
class ErrorResponseJsonTests {

  /**
   * Basic test that verifies correct json can be parsed
   */
  @Test
  @DisplayName("Basic Test")
  fun createTest1() {
    val jsonString = """
    {
      "errors": [{
        "id": "228afb9e-58fa-4246-8fed-c0dec1f23595",
        "status": "404",
        "code": "NOT_FOUND",
        "title": "The specified resource does not exist",
        "detail": "There is no resource of type 'submissions' with id '5685647e-0125-4343-a068-1c5786499827'"
      }]
    }
    """.trimIndent()

    val errorResponseJsonResult = ErrorResponseJson.create(jsonString)
    assertThat(errorResponseJsonResult).isOk()

    errorResponseJsonResult.onSuccess { errorResponseJson ->
      assertThat(errorResponseJson.errors).hasSize(1)
      assertThat(errorResponseJson.errors[0]).prop(ErrorJson::id).isEqualTo("228afb9e-58fa-4246-8fed-c0dec1f23595")
      assertThat(errorResponseJson.errors[0]).prop(ErrorJson::status).isEqualTo("404")
      assertThat(errorResponseJson.errors[0]).prop(ErrorJson::code).isEqualTo("NOT_FOUND")
      assertThat(errorResponseJson.errors[0]).prop(ErrorJson::title).isEqualTo("The specified resource does not exist")
      assertThat(errorResponseJson.errors[0]).prop(ErrorJson::detail)
        .isEqualTo("There is no resource of type 'submissions' with id '5685647e-0125-4343-a068-1c5786499827'")
    }
  }

  /**
   * Attempts to create a [ErrorResponseJson] from a jsonString that is `null`.
   * Verifies that an Error is returned
   */
  @Test
  @DisplayName("null jsonString Test")
  fun createTest2() {
    val jsonString: String? = null
    val errorResponseJsonResult = ErrorResponseJson.create(jsonString)
    assertThat(errorResponseJsonResult).isErr()
    errorResponseJsonResult.onFailure { jsonParseError ->
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::msg)
        .isEqualTo("Json String is <null> or empty.")
    }
  }

  /**
   * Attempts to create a [ErrorResponseJson] from a jsonString that is empty.
   * Verifies that an Error is returned
   */
  @Test
  @DisplayName("Empty jsonString Test")
  fun createTest3() {
    val jsonString = ""
    val errorResponseJsonResult = ErrorResponseJson.create(jsonString)
    assertThat(errorResponseJsonResult).isErr()
    errorResponseJsonResult.onFailure { jsonParseError ->
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::msg)
        .isEqualTo("Json String is <null> or empty.")
    }
  }

  /**
   * Attempts to create a [ErrorResponseJson] from a jsonString that is valid json but not the
   * expected json.
   * Verifies that an Error is returned
   */
  @Test
  @DisplayName("Unexpected jsonString Test")
  fun createTest4() {
    val jsonString = """
    {
      "errors": [{
        "id": "228afb9e-58fa-4246-8fed-c0dec1f23595",
        "status_code": "404",
        "status_message": "NOT_FOUND",
        "title": "The specified resource does not exist",
        "detail": "There is no resource of type 'submissions' with id '5685647e-0125-4343-a068-1c5786499827'"
      }]
    }
    """.trimIndent()
    val errorResponseJsonResult = ErrorResponseJson.create(jsonString)
    assertThat(errorResponseJsonResult).isErr()
    errorResponseJsonResult.onFailure { jsonParseError ->
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::msg)
        .isEqualTo("Error parsing json: Cannot skip unexpected NAME at \$.errors[0].status_code.")
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::jsonString)
        .isEqualTo(jsonString)
    }
  }

  /**
   * Attempts to create a [ErrorResponseJson] from a jsonString contains invalid json
   * Verifies that an Error is returned
   */
  @Test
  @DisplayName("invalid jsonString Test")
  fun createTest5() {
    val jsonString = "I am Jason!"
    val errorResponseJsonResult = ErrorResponseJson.create(jsonString)
    assertThat(errorResponseJsonResult).isErr()
    errorResponseJsonResult.onFailure { jsonParseError ->
      log.info(errorResponseJsonResult.getError().toString())
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::msg)
        .isEqualTo("Error parsing json: Expected BEGIN_OBJECT but was STRING at path \$.")
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::jsonString)
        .isEqualTo(jsonString)
    }
  }
}
