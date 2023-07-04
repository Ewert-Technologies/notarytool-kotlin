package ca.ewert.notarytoolkotlin.json.notaryapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import ca.ewert.notarytoolkotlin.NotaryToolError
import ca.ewert.notarytoolkotlin.isErrAnd
import ca.ewert.notarytoolkotlin.isOk
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Unit tests for [SubmissionLogUrlResponseJson]
 *
 * @author Victor Ewert
 */
class SubmissionLogUrlResponseJsonTest {

  /**
   * Tests converting a sample SubmissionLogUrlResponseJson json String to a [SubmissionLogUrlResponseJson] using
   * the [SubmissionLogUrlResponseJson.create] method.
   */
  @Test
  @DisplayName("Basic Test")
  fun createTest1() {
    val jsonString = """
    {
      "data": {
        "id": "b014d72f-17b6-45ac-abdf-8f39b9241c58",
        "type": "submissionsLog",
        "attributes": {
          "developerLogUrl": "https://notary-artifacts-prod.s3.amazonaws.com/prod/b014d72f-17b6-45ac-abdf-8f39b9241c58/developer_log.json?AWSAccessKeyId=ASIARQRX7CZSS7QQBDXO&Signature=fri%2FbHKIn7QEQaJRYHx09BfL15w%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEKT%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJHMEUCIQDQlWlN8SD55G5T6%2FvKyJsxtNhoY5RTUJAJ90nAcPb9fAIgc6Slsa4Ofc1BNZl7ORBNoRnJ4jYqeC240O1ObkDKvVIqkQMIHBADGgwxMDQyNzAzMzc2MzciDMmsmWY1oZdpGhWbhSruAgiPonw%2FXkvDP9W8ad4tky%2BWjTwWY3YH3BbztUof8bHWv1YI3%2FxHsLaHQYl8m9HbEF3RWLevJQAkFaRAjufZD6AH8WjDI2%2BU%2BC1ssKrZrpD1Ijwwnz%2FgUd62sjUQxveBgqnoICk%2BoptbTBXAMNeh1rDjE1q6kbgQ7nXb8J%2BNSthO6LwzpElh967JciwdOZRp6Qia71xRlsKtga7Oe9HuJcdnIt9T9MaXJhv3YGM9ZKVfjS9a4CCoWlp9bU1Wp%2FoSjsXxzLs6jzbLzF%2FCdVniMl2HkVEvz8wvlNsVBwSdmLtlM%2B7Jja9clxQTwlUAKphvXFEstdgDZf%2FSqFBmbY0Zc6QAhy5C43NtEZAduYEiXG3supKVfmlzkeHtoReFYGFGxAmbZ%2BHuCpFg8jjM5kJhS8khmknKvXFlrAhJRpPxDmIhqzqvZIEMWVuT2%2FNjNQaLtNNSUPGomvLzdGbwlf9fD0MGQAvXhexBZmxPIzHVvTCy4pGlBjqdASt5YQqQrxjgC5l91SJO1qrxHFoEtrQiNgHvZks9Ofs1AhvYib3mYzkXzH%2BLMQWHDDXTfQ8Z8%2F06Ed2Z1d2L9dHvBb3HAeRWEgL3V9Nz4ZySsuyb6ToSaBEdgA59mUQU4jMNWJ0AEpczUqbhN8aVu4UmrpGFisIoagYN6IkANb3VaeMIMtWaWKQOpSX%2F1Vr429oAOZ5fnl4h8tnD06A%3D&Expires=1688508160"
        }
      },
      "meta": {}
    }  
    """.trimIndent()

    val submissionLogUrlResponseJsonResult = SubmissionLogUrlResponseJson.create(jsonString)

    assertThat(submissionLogUrlResponseJsonResult).isOk()

    submissionLogUrlResponseJsonResult.onSuccess { submissionLogUrlResponseJson ->
      assertThat(submissionLogUrlResponseJson).isNotNull()
      assertThat(submissionLogUrlResponseJson.submissionLogResponseData.id).isEqualTo("b014d72f-17b6-45ac-abdf-8f39b9241c58")
      assertThat(submissionLogUrlResponseJson.submissionLogResponseData.type).isEqualTo("submissionsLog")
      assertThat(submissionLogUrlResponseJson.submissionLogResponseData.attributes.developerLogUrl)
        .isEqualTo(
          "https://notary-artifacts-prod.s3.amazonaws.com/prod/b014d72f-17b6-45ac-abdf-8f39b9241c58/developer_log.json?AWSAccessKeyId=ASIARQRX7CZSS7QQBDXO&Signature=fri%2FbHKIn7QEQaJRYHx09BfL15w%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEKT%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLXdlc3QtMiJHMEUCIQDQlWlN8SD55G5T6%2FvKyJsxtNhoY5RTUJAJ90nAcPb9fAIgc6Slsa4Ofc1BNZl7ORBNoRnJ4jYqeC240O1ObkDKvVIqkQMIHBADGgwxMDQyNzAzMzc2MzciDMmsmWY1oZdpGhWbhSruAgiPonw%2FXkvDP9W8ad4tky%2BWjTwWY3YH3BbztUof8bHWv1YI3%2FxHsLaHQYl8m9HbEF3RWLevJQAkFaRAjufZD6AH8WjDI2%2BU%2BC1ssKrZrpD1Ijwwnz%2FgUd62sjUQxveBgqnoICk%2BoptbTBXAMNeh1rDjE1q6kbgQ7nXb8J%2BNSthO6LwzpElh967JciwdOZRp6Qia71xRlsKtga7Oe9HuJcdnIt9T9MaXJhv3YGM9ZKVfjS9a4CCoWlp9bU1Wp%2FoSjsXxzLs6jzbLzF%2FCdVniMl2HkVEvz8wvlNsVBwSdmLtlM%2B7Jja9clxQTwlUAKphvXFEstdgDZf%2FSqFBmbY0Zc6QAhy5C43NtEZAduYEiXG3supKVfmlzkeHtoReFYGFGxAmbZ%2BHuCpFg8jjM5kJhS8khmknKvXFlrAhJRpPxDmIhqzqvZIEMWVuT2%2FNjNQaLtNNSUPGomvLzdGbwlf9fD0MGQAvXhexBZmxPIzHVvTCy4pGlBjqdASt5YQqQrxjgC5l91SJO1qrxHFoEtrQiNgHvZks9Ofs1AhvYib3mYzkXzH%2BLMQWHDDXTfQ8Z8%2F06Ed2Z1d2L9dHvBb3HAeRWEgL3V9Nz4ZySsuyb6ToSaBEdgA59mUQU4jMNWJ0AEpczUqbhN8aVu4UmrpGFisIoagYN6IkANb3VaeMIMtWaWKQOpSX%2F1Vr429oAOZ5fnl4h8tnD06A%3D&Expires=1688508160",
        )
    }
  }

  /**
   * Tests attempting to create a [SubmissionLogUrlResponseJson] object using a json String
   * that is `null`. Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("null json String test")
  fun createTest2() {
    val jsonString: String? = null
    val submissionLogUrlResponseJsonResult = SubmissionLogUrlResponseJson.create(jsonString)

    assertThat(submissionLogUrlResponseJsonResult).isErrAnd().prop(NotaryToolError.JsonParseError::msg)
      .isEqualTo("Json String is <null> or empty.")
  }

  /**
   * Tests attempting to create a [SubmissionLogUrlResponseJson] object using a json String
   * that is empty. Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("empty json String test")
  fun createTest3() {
    val jsonString = ""
    val submissionLogUrlResponseJsonResult = SubmissionLogUrlResponseJson.create(jsonString)

    assertThat(submissionLogUrlResponseJsonResult).isErrAnd().prop(NotaryToolError.JsonParseError::msg)
      .isEqualTo("Json String is <null> or empty.")
  }

  /**
   * Tests attempting to create a [SubmissionLogUrlResponseJson] that is valid json
   * but not the expected json. Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("Unexpected json String test")
  fun createTest4() {
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
    val submissionLogUrlResponseJsonResult = SubmissionLogUrlResponseJson.create(jsonString)

    assertThat(submissionLogUrlResponseJsonResult).isErrAnd().prop(NotaryToolError.JsonParseError::msg)
      .isEqualTo("Error parsing json: Cannot skip unexpected NAME at \$.data.attributes.createdDate.")
  }

  /**
   * Tests attempting to create a [SubmissionLogUrlResponseJson] that invalid json.
   * Asserts that an Error result is returned.
   */
  @Test
  @DisplayName("invalid json String test")
  fun createTest5() {
    val jsonString = """
    !Json
    """.trimIndent()
    val submissionLogUrlResponseJsonResult = SubmissionLogUrlResponseJson.create(jsonString)

    assertThat(submissionLogUrlResponseJsonResult).isErrAnd().prop(NotaryToolError.JsonParseError::msg)
      .isEqualTo("Error parsing json: Expected BEGIN_OBJECT but was STRING at path \$.")
  }
}
