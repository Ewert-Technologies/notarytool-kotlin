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
 * Unit Tests for [SubmissionLogUrlResponseJson]
 *
 * @author vewert
 */
class SubmissionLogUrlResponseJsonTests {

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val jsonAdapter: JsonAdapter<SubmissionLogUrlResponseJson> =
    moshi.adapter(SubmissionLogUrlResponseJson::class.java)


  /**
   * Tests converting a sample SubmissionLogUrlResponse json String to a [SubmissionLogUrlResponseJson]
   */
  @Test
  fun fromJsonTest1() {
    val jsonString = """
    {
      "data": {
        "attributes": {
          "developerLogUrl": "https://notary-artifacts-prod.s3.amazonaws.com/prod/b014d72f-17b6-45ac-abdf-8f39b9241c58/developer_log.json?AWSAccessKeyId=ASIARQRX7CZST6PU634N&Signature=1GdNjbqIqkDpSZP5Rs57Dpc9yHw%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEN%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJIMEYCIQCkj0cKlot%2FxqJi9f1ew5XQZwbsnbEIj9D0Bizo8ohk3wIhAJceCiugUw7e8VDFlKhlSy8FG3nQSw3%2FzUC1gWu4iWQcKpEDCDgQAxoMMTA0MjcwMzM3NjM3Igzzc3QFRnBDz1lBY6Iq7gKduPAPrb6CaTR3engQ9AaWtNft%2Feg3BVXgdo6cCHnI9TPyCVZ6m8ULrvy3nr2jCYebRdot%2Fw%2FlDXMVEMAMhsIIRJL3OUpkUJwp7GDCHGIVntLOhcuMuF3wVa9OKOjWcEWTpMj%2F8aY%2FHngZ1PPH8P%2F2VVhW1CwIYB7KE%2F26Jxpn9Pm1YgGcAwC1DNsmD5hMeRanPMJkUyNQfdeCSL2fq46Fl%2Bkx3IkF%2BaQoHNDD0RE%2FO9JfTqcxBRoWimtGQO4%2F5lt2E%2Fmmss5Kxw%2FfL%2B9gyX3Z5hPe%2Br1oF8j4wpRPxVSWm2s94sIfjXq8TTlfMTlZEVFlv9hP8sCfehApcuOAEb1yCj4mbCwH5TG2NBncH%2BegreqSuFe2fVuVbBHF6GCM5GOm5xO1hXTGwDiixW%2BGP5flb3RUBxIu9udqDhenirGjLWiiAu8sPuDGMmjsOzWoBILBR0JqolcAqQx6blbnIsH%2BRhtl5Q%2BeMO8vVWjm%2BHww5qSupAY6nAFLWWhApyHJFDyDO%2FKTWK5VbXH%2FYxGT8AxbvhPQtX8mqz%2B6tGeBfy579ivqJvpQ1jexPvBkARTe2XPEP1sQd6Qd02f5V1wUikgrNdmaJWO%2FrQndAfKVPs4HyZpO1C%2BI2tKlJDNFiPgyOQrmcStMbIctZs6N3jvMK8Qyp3%2F9KZ6klc7C75kWrvYRvrQmvUoOqf8CpeN4XAu89T3ig1M%3D&Expires=1686872935"
        },
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog"
      },
      "meta": {}
    }
    """.trimIndent()

    val submissionLogUrlResponseJson: SubmissionLogUrlResponseJson? = jsonAdapter.fromJson(jsonString)
    log.info { submissionLogUrlResponseJson.toString() }
    assertThat(submissionLogUrlResponseJson).isNotNull()
    assertThat(submissionLogUrlResponseJson?.submissionLogResponseData?.attributes?.developerLogUrl).isEqualTo("https://notary-artifacts-prod.s3.amazonaws.com/prod/b014d72f-17b6-45ac-abdf-8f39b9241c58/developer_log.json?AWSAccessKeyId=ASIARQRX7CZST6PU634N&Signature=1GdNjbqIqkDpSZP5Rs57Dpc9yHw%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEN%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJIMEYCIQCkj0cKlot%2FxqJi9f1ew5XQZwbsnbEIj9D0Bizo8ohk3wIhAJceCiugUw7e8VDFlKhlSy8FG3nQSw3%2FzUC1gWu4iWQcKpEDCDgQAxoMMTA0MjcwMzM3NjM3Igzzc3QFRnBDz1lBY6Iq7gKduPAPrb6CaTR3engQ9AaWtNft%2Feg3BVXgdo6cCHnI9TPyCVZ6m8ULrvy3nr2jCYebRdot%2Fw%2FlDXMVEMAMhsIIRJL3OUpkUJwp7GDCHGIVntLOhcuMuF3wVa9OKOjWcEWTpMj%2F8aY%2FHngZ1PPH8P%2F2VVhW1CwIYB7KE%2F26Jxpn9Pm1YgGcAwC1DNsmD5hMeRanPMJkUyNQfdeCSL2fq46Fl%2Bkx3IkF%2BaQoHNDD0RE%2FO9JfTqcxBRoWimtGQO4%2F5lt2E%2Fmmss5Kxw%2FfL%2B9gyX3Z5hPe%2Br1oF8j4wpRPxVSWm2s94sIfjXq8TTlfMTlZEVFlv9hP8sCfehApcuOAEb1yCj4mbCwH5TG2NBncH%2BegreqSuFe2fVuVbBHF6GCM5GOm5xO1hXTGwDiixW%2BGP5flb3RUBxIu9udqDhenirGjLWiiAu8sPuDGMmjsOzWoBILBR0JqolcAqQx6blbnIsH%2BRhtl5Q%2BeMO8vVWjm%2BHww5qSupAY6nAFLWWhApyHJFDyDO%2FKTWK5VbXH%2FYxGT8AxbvhPQtX8mqz%2B6tGeBfy579ivqJvpQ1jexPvBkARTe2XPEP1sQd6Qd02f5V1wUikgrNdmaJWO%2FrQndAfKVPs4HyZpO1C%2BI2tKlJDNFiPgyOQrmcStMbIctZs6N3jvMK8Qyp3%2F9KZ6klc7C75kWrvYRvrQmvUoOqf8CpeN4XAu89T3ig1M%3D&Expires=1686872935")
    assertThat(submissionLogUrlResponseJson?.submissionLogResponseData?.id).isEqualTo("b014d72f-17b6-45ac-abdf-8f39b9241c58")
    assertThat(submissionLogUrlResponseJson?.submissionLogResponseData?.type).isEqualTo("submissionsLog")


  }
}