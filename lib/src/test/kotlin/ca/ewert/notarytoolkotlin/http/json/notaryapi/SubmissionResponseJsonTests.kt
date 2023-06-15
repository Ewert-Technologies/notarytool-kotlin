package ca.ewert.notarytoolkotlin.http.json.notaryapi

import assertk.assertThat
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
 * Unit Tests for [SubmissionResponseJson]
 *
 * @author vewert
 */
class SubmissionResponseJsonTests {

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val jsonAdapter: JsonAdapter<SubmissionResponseJson> = moshi.adapter(SubmissionResponseJson::class.java)

  /**
   * Tests converting a sample SubmissionResponse json String to a [SubmissionResponseJson]
   */
  @Test
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
}