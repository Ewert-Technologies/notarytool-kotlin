package ca.ewert.notarytoolkotlin.http.json.notaryapi

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import mu.KotlinLogging
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
   * Tests converting a sample SubmissionListResponse json String to a [SubmissionListResponseJson]
   */
  @Test
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
    log.info(submissionListResponseJson.toString())
    assertThat(submissionListResponseJson).isNotNull()

    if (submissionListResponseJson != null) {
      val dataList: List<SubmissionsDataJson> = submissionListResponseJson.submissionListResponseData
      assertThat(dataList).hasSize(3)
      assertThat(dataList[1].attributesJson.createdDate).isEqualTo("2021-04-23T17:44:54.761Z")
      assertThat(dataList[1].attributesJson.name).isEqualTo("OvernightTextEditor_11.6.7.zip")
      assertThat(dataList[1].attributesJson.status).isEqualTo("Accepted")
      assertThat(dataList[1].id).isEqualTo("cf0c235a-dad2-4c24-96eb-c876d4cb3a2d")
      assertThat(dataList[1].type).isEqualTo("submissions")
    }
  }
}