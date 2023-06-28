package ca.ewert.notarytoolkotlin.http.json.notaryapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [NewSubmissionResponseJson]
 *
 * @author vewert
 */
class NewSubmissionResponseJsonTests {

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val jsonAdapter: JsonAdapter<NewSubmissionResponseJson> =
    moshi.adapter(NewSubmissionResponseJson::class.java)

  /**
   * Tests converting a sample NewSubmissionResponse json String to a [NewSubmissionResponseJson]
   */
  @Test
  fun fromJsonTest1() {
    val jsonString = """
    {
      "data": {
        "attributes": {
          "awsAccessKeyId": "ASIAIOSFODNN7EXAMPLE",
          "awsSecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
          "awsSessionToken": "AQoDYXdzEJr...",
          "bucket": "EXAMPLE-BUCKET",
          "object": "EXAMPLE-KEY-NAME"
        },
        "id": "2efe2717-52ef-43a5-96dc-0797e4ca1041",
        "type": "newSubmissions"
      },
      "meta": {}
    }
    """.trimIndent()

    val newSubmissionResponseJson: NewSubmissionResponseJson? = jsonAdapter.fromJson(jsonString)
    log.info { newSubmissionResponseJson.toString() }
    assertThat(newSubmissionResponseJson).isNotNull()
    assertThat(newSubmissionResponseJson?.newSubmissionResponseData?.attributes?.awsAccessKeyId)
      .isEqualTo("ASIAIOSFODNN7EXAMPLE")
    assertThat(newSubmissionResponseJson?.newSubmissionResponseData?.attributes?.awsSecretAccessKey)
      .isEqualTo("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
    assertThat(newSubmissionResponseJson?.newSubmissionResponseData?.attributes?.bucket)
      .isEqualTo("EXAMPLE-BUCKET")
    assertThat(newSubmissionResponseJson?.newSubmissionResponseData?.attributes?.objectKey)
      .isEqualTo("EXAMPLE-KEY-NAME")
    assertThat(newSubmissionResponseJson?.newSubmissionResponseData?.id)
      .isEqualTo("2efe2717-52ef-43a5-96dc-0797e4ca1041")
    assertThat(newSubmissionResponseJson?.newSubmissionResponseData?.type)
      .isEqualTo("newSubmissions")
  }
}
