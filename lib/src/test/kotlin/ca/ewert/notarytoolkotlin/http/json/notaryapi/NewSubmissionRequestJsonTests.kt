package ca.ewert.notarytoolkotlin.http.json.notaryapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import mu.KotlinLogging
import org.junit.jupiter.api.Test

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [NewSubmissionRequestJson]
 *
 *
 * @author vewert
 */
class NewSubmissionRequestJsonTests {

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val jsonAdapter: JsonAdapter<NewSubmissionRequestJson> =
    moshi.adapter(NewSubmissionRequestJson::class.java).indent("  ")

  /**
   * Test creating a NewSubmissionRequest json String from a [NewSubmissionRequestJson] object.
   */
  @Test
  fun toJsonTest1() {
    val newSubmissionRequestNotificationJson =
      NewSubmissionRequestNotificationJson(channel = "webhook", target = "https://example.com")
    val newSubmissionRequestJson = NewSubmissionRequestJson(
      listOf(newSubmissionRequestNotificationJson),
      sha256 = "68d561c564ef61f718e99a81b13bcb52af11b7ac9baf538af3ea0c83326fb6a1",
      submissionName = "OvernightTextEditor_11.6.8.zip"
    )

    val jsonString = jsonAdapter.toJson(newSubmissionRequestJson)
    log.info { "jsonString:\n$jsonString" }

    val expectedJsonString = """
    {
      "notifications": [
        {
          "channel": "webhook",
          "target": "https://example.com"
        }
      ],
      "sha256": "68d561c564ef61f718e99a81b13bcb52af11b7ac9baf538af3ea0c83326fb6a1",
      "submissionName": "OvernightTextEditor_11.6.8.zip"
    }
    """.trimIndent()

    assertThat(jsonString).isEqualTo(expectedJsonString)
  }

  /**
   * Test creating a NewSubmissionRequest json String from a [NewSubmissionRequestJson] object with no Notifications
   */
  @Test
  fun toJsonTest2() {
    val newSubmissionRequestJson = NewSubmissionRequestJson(
      emptyList(),
      sha256 = "68d561c564ef61f718e99a81b13bcb52af11b7ac9baf538af3ea0c83326fb6a1",
      submissionName = "OvernightTextEditor_11.6.8.zip"
    )

    val jsonString = jsonAdapter.toJson(newSubmissionRequestJson)
    log.info { "jsonString:\n$jsonString" }

    val expectedJsonString = """
    {
      "notifications": [],
      "sha256": "68d561c564ef61f718e99a81b13bcb52af11b7ac9baf538af3ea0c83326fb6a1",
      "submissionName": "OvernightTextEditor_11.6.8.zip"
    }
    """.trimIndent()

    assertThat(jsonString).isEqualTo(expectedJsonString)
  }


}