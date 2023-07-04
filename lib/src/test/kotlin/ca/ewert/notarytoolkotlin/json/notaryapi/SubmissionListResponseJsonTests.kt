package ca.ewert.notarytoolkotlin.json.notaryapi

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.fail
import ca.ewert.notarytoolkotlin.isErr
import com.github.michaelbull.result.getError
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
 * Unit tests for [SubmissionListResponseJson]
 *
 * @author vewert
 */
class SubmissionListResponseJsonTests {

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val jsonAdapter: JsonAdapter<SubmissionListResponseJson> =
    moshi.adapter(SubmissionListResponseJson::class.java)

  /**
   * Tests converting a sample valid SubmissionListResponse json String to a [SubmissionListResponseJson]
   * directly using adapter
   */
  @Test
  @DisplayName("Direct create test")
  fun fromJsonTest1() {
    val jsonString = """
      {
        "data": [
          {
            "attributes": {
              "createdDate": "2021-04-29T01:38:09.498Z",
              "name": "OvernightTextEditor_11.6.8.zip",
              "status": "Accepted"
            },
            "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
            "type": "submissions"
          },
          {
            "attributes": {
              "createdDate": "2021-04-23T17:44:54.761Z",
              "name": "OvernightTextEditor_11.6.7.zip",
              "status": "Accepted"
            },
            "id": "cf0c235a-dad2-4c24-96eb-c876d4cb3a2d",
            "type": "submissions"
          },
          {
            "attributes": {
              "createdDate": "2021-04-19T16:56:17.839Z",
              "name": "OvernightTextEditor_11.6.7.zip",
              "status": "Invalid"
            },
            "id": "38ce81cc-0bf7-454b-91ef-3f7395bf297b",
            "type": "submissions"
          }
        ],
        "meta": {}
      }
    """.trimIndent()

    val submissionListResponseJson: SubmissionListResponseJson? = jsonAdapter.fromJson(jsonString)
    log.info { submissionListResponseJson.toString() }
    assertThat(submissionListResponseJson).isNotNull()

    if (submissionListResponseJson != null) {
      val dataList: List<SubmissionsDataJson> = submissionListResponseJson.submissionListResponseData
      assertThat(dataList).hasSize(3)
      assertThat(dataList[1].attributes.createdDate).isEqualTo("2021-04-23T17:44:54.761Z")
      assertThat(dataList[1].attributes.name).isEqualTo("OvernightTextEditor_11.6.7.zip")
      assertThat(dataList[1].attributes.status).isEqualTo("Accepted")
      assertThat(dataList[1].id).isEqualTo("cf0c235a-dad2-4c24-96eb-c876d4cb3a2d")
      assertThat(dataList[1].type).isEqualTo("submissions")
    }
  }

  /**
   * Tests converting a sample valid SubmissionListResponse json String to a [SubmissionListResponseJson]
   * by using [SubmissionListResponseJson.create] method
   */
  @Test
  @DisplayName("Basic Test")
  fun createTest1() {
    val jsonString = """
      {
        "data": [
          {
            "attributes": {
              "createdDate": "2021-04-29T01:38:09.498Z",
              "name": "OvernightTextEditor_11.6.8.zip",
              "status": "Accepted"
            },
            "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
            "type": "submissions"
          },
          {
            "attributes": {
              "createdDate": "2021-04-23T17:44:54.761Z",
              "name": "OvernightTextEditor_11.6.7.zip",
              "status": "Accepted"
            },
            "id": "cf0c235a-dad2-4c24-96eb-c876d4cb3a2d",
            "type": "submissions"
          },
          {
            "attributes": {
              "createdDate": "2021-04-19T16:56:17.839Z",
              "name": "OvernightTextEditor_11.6.7.zip",
              "status": "Invalid"
            },
            "id": "38ce81cc-0bf7-454b-91ef-3f7395bf297b",
            "type": "submissions"
          }
        ],
        "meta": {}
      }
    """.trimIndent()

    val submissionListResponseJsonResult = SubmissionListResponseJson.create(jsonString)

    submissionListResponseJsonResult.onSuccess { submissionListResponseJson ->
      val dataList: List<SubmissionsDataJson> = submissionListResponseJson.submissionListResponseData
      assertThat(dataList).hasSize(3)
      assertThat(dataList[1].attributes.createdDate).isEqualTo("2021-04-23T17:44:54.761Z")
      assertThat(dataList[1].attributes.name).isEqualTo("OvernightTextEditor_11.6.7.zip")
      assertThat(dataList[1].attributes.status).isEqualTo("Accepted")
      assertThat(dataList[1].id).isEqualTo("cf0c235a-dad2-4c24-96eb-c876d4cb3a2d")
      assertThat(dataList[1].type).isEqualTo("submissions")
    }

    submissionListResponseJsonResult.onFailure { jsonParseError ->
      fail(AssertionError(jsonParseError))
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
    val submissionListResponseJsonResult = SubmissionListResponseJson.create(jsonString)

    assertThat(submissionListResponseJsonResult).isErr()
    log.info { submissionListResponseJsonResult.getError().toString() }
  }

  /**
   * Tests attempting to create a [SubmissionListResponseJson] object using an empty json String.
   * Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("Empty jsonString Test")
  fun createTest3() {
    val jsonString = ""
    val submissionListResponseJsonResult = SubmissionListResponseJson.create(jsonString)

    assertThat(submissionListResponseJsonResult).isErr()
    log.info { submissionListResponseJsonResult.getError().toString() }
  }

  /**
   * Tests attempting to create a [SubmissionListResponseJson] object using a String that is valid json,
   * but not the expected json. Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("Unexpected jsonString Test")
  fun createTest4() {
    val jsonString = """
      {
        "hello": "world"
      }
    """.trimIndent()
    val submissionListResponseJsonResult = SubmissionListResponseJson.create(jsonString)

    assertThat(submissionListResponseJsonResult).isErr()
    log.info { submissionListResponseJsonResult.getError().toString() }
  }

  /**
   * Tests attempting to create a [SubmissionListResponseJson] object using a String that is not valid json.
   * Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("Invalid jsonString Test")
  fun createTest5() {
    val jsonString = "Not Authorized"
    val submissionListResponseJsonResult = SubmissionListResponseJson.create(jsonString)

    assertThat(submissionListResponseJsonResult).isErr()
    log.info { submissionListResponseJsonResult.getError().toString() }
  }
}
