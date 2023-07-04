package ca.ewert.notarytoolkotlin.json.notaryapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import ca.ewert.notarytoolkotlin.isErr
import ca.ewert.notarytoolkotlin.isOk
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [SubmissionResponseJson]
 *
 * @author vewert
 */
class SubmissionResponseJsonTests {

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val jsonAdapter: JsonAdapter<SubmissionResponseJson> = moshi.adapter(SubmissionResponseJson::class.java)

  /**
   * Tests converting a sample SubmissionResponse json String to a [SubmissionResponseJson] directly
   * using moshi
   */
  @Test
  @DisplayName("Direct create test")
  fun fromJsonTest1() {
    val jsonString = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "Accepted"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    val submissionResponseJson: SubmissionResponseJson? = jsonAdapter.fromJson(jsonString)
    log.info { submissionResponseJson.toString() }
    assertThat(submissionResponseJson).isNotNull()
    assertThat(submissionResponseJson?.submissionResponseData?.attributes?.createdDate).isEqualTo("2022-06-08T01:38:09.498Z")
    assertThat(submissionResponseJson?.submissionResponseData?.attributes?.name).isEqualTo("OvernightTextEditor_11.6.8.zip")
    assertThat(submissionResponseJson?.submissionResponseData?.attributes?.status).isEqualTo("Accepted")
    assertThat(submissionResponseJson?.submissionResponseData?.id).isEqualTo("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    assertThat(submissionResponseJson?.submissionResponseData?.type).isEqualTo("submissions")
  }

  /**
   * Tests converting a sample SubmissionResponse json String to a [SubmissionResponseJson] using
   * the [SubmissionResponseJson.create] method.
   */
  @Test
  @DisplayName("Basic Test")
  fun createTest1() {
    val jsonString = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status": "Accepted"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()

    val submissionResponseJsonResult = SubmissionResponseJson.create(jsonString)

    assertThat(submissionResponseJsonResult).isOk()

    submissionResponseJsonResult.onSuccess { submissionResponseJson ->
      log.info { submissionResponseJson.toString() }
      assertThat(submissionResponseJson).isNotNull()
      assertThat(submissionResponseJson.submissionResponseData.attributes.createdDate).isEqualTo("2022-06-08T01:38:09.498Z")
      assertThat(submissionResponseJson.submissionResponseData.attributes.name).isEqualTo("OvernightTextEditor_11.6.8.zip")
      assertThat(submissionResponseJson.submissionResponseData.attributes.status).isEqualTo("Accepted")
      assertThat(submissionResponseJson.submissionResponseData.id).isEqualTo("2efe2717-52ef-43a5-96dc-0797e4ca1041")
      assertThat(submissionResponseJson.submissionResponseData.type).isEqualTo("submissions")
    }
  }

  /**
   * Tests attempting to create a [SubmissionListResponseJson] object using a json String
   * that is `null`. Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("null jsonString Test")
  fun createTest2() {
    val jsonString = null
    val submissionResponseJsonResult = SubmissionResponseJson.create(jsonString)
    assertThat(submissionResponseJsonResult).isErr()
    submissionResponseJsonResult.onFailure { jsonParseError ->
      log.info { jsonParseError.toString() }
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::msg)
        .isEqualTo("Json String is <null> or empty.")
    }
  }

  /**
   * Tests attempting to create a [SubmissionListResponseJson] object using a json String
   * that is emtpy. Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("Empty jsonString Test")
  fun createTest3() {
    val jsonString = ""
    val submissionResponseJsonResult = SubmissionResponseJson.create(jsonString)
    assertThat(submissionResponseJsonResult).isErr()
    submissionResponseJsonResult.onFailure { jsonParseError ->
      log.info { jsonParseError.toString() }
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::msg)
        .isEqualTo("Json String is <null> or empty.")
    }
  }

  /**
   * Attempts to create a [SubmissionResponseJson] from a jsonString that is valid json but not the
   * expected json.
   * Verifies that an Error is returned
   */
  @Test
  @DisplayName("Unexpected jsonString Test")
  fun createTest4() {
    val jsonString = """
    {
      "data": {
        "attributes": {
          "createdDate": "2022-06-08T01:38:09.498Z",
          "name": "OvernightTextEditor_11.6.8.zip",
          "status code": "Accepted"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "submissions"
      },
      "meta": {}
    }
    """.trimIndent()
    val errorResponseJsonResult = SubmissionResponseJson.create(jsonString)
    assertThat(errorResponseJsonResult).isErr()
    errorResponseJsonResult.onFailure { jsonParseError ->
      log.info { jsonParseError.toString() }
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::msg)
        .isEqualTo("Error parsing json: Cannot skip unexpected NAME at \$.data.attributes.status code.")
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::jsonString)
        .isEqualTo(jsonString)
    }
  }

  /**
   * Attempts to create a [SubmissionResponseJson] from a jsonString that is invalid json
   * Verifies that an Error is returned
   */
  @Test
  @DisplayName("Invalid jsonString Test")
  fun createTest5() {
    val jsonString = """
    I am Jason,
    not json
    """.trimIndent()
    val errorResponseJsonResult = SubmissionResponseJson.create(jsonString)
    assertThat(errorResponseJsonResult).isErr()
    errorResponseJsonResult.onFailure { jsonParseError ->
      log.info { jsonParseError.toString() }
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::msg)
        .isEqualTo("Error parsing json: Expected BEGIN_OBJECT but was STRING at path \$.")
      assertThat(jsonParseError).prop(NotaryToolError.JsonParseError::jsonString)
        .isEqualTo(jsonString)
    }
  }
}
