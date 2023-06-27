package ca.ewert.notarytoolkotlin.http.json

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import ca.ewert.notarytoolkotlin.http.json.jwt.JwtHeaderJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Logging Object */
private val log = KotlinLogging.logger {}

/**
 * Unit Tests for [JwtHeaderJson]
 *
 * @author vewert
 */
class JwtHeaderJsonTests {

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val jsonAdapter: JsonAdapter<JwtHeaderJson> = moshi.adapter(JwtHeaderJson::class.java).indent("  ")

  /**
   * Called before each test
   */
  @BeforeEach
  fun setUp() {
  }

  /**
   * Called after each test
   */
  @AfterEach
  fun tearDown() {
  }

  /**
   * Basic parsing Test
   */
  @Test
  fun parseTest1() {
    val jsonString = """
    {
      "alg": "ES256",
      "kid": "2X9R4HXF34",
      "typ": "JWT"
    }
    """.trimIndent()

    val jwtHeaderJson = jsonAdapter.fromJson(jsonString)
    assertThat(jwtHeaderJson?.alg).isEqualTo("ES256")
    assertThat(jwtHeaderJson?.kid).isEqualTo("2X9R4HXF34")
    assertThat(jwtHeaderJson?.typ).isEqualTo("JWT")
  }

  /**
   * Parse with empty value
   */
  @Test
  fun parseTest2() {
    val jsonString = """
    {
      "alg": "",
      "kid": "2X9R4HXF34",
      "typ": "JWT"
    }
    """.trimIndent()

    val jwtHeaderJson = jsonAdapter.fromJson(jsonString)
    assertThat(jwtHeaderJson?.alg).isEqualTo("")
    assertThat(jwtHeaderJson?.kid).isEqualTo("2X9R4HXF34")
    assertThat(jwtHeaderJson?.typ).isEqualTo("JWT")
  }

  /**
   * Parse with missing value. This is expected to throw an Exception.
   */
  @Test
  fun parseTest3() {
    val jsonString = """
    {
      "kid": "2X9R4HXF34",
      "typ": "JWT"
    }
    """.trimIndent()

    assertFailure {
      jsonAdapter.fromJson(jsonString)
    }.hasMessage("Required value 'alg' missing at \$")
  }

  /**
   * Basic Serializing to json String test.
   */
  @Test
  fun serializeTest1() {
    val jwtHeaderJson = JwtHeaderJson(alg = "ES256", kid = "2X9R4HXF34", typ = "JWT")
    val jsonString = jsonAdapter.toJson(jwtHeaderJson)
    val expectedJsonString = """
    {
      "alg": "ES256",
      "kid": "2X9R4HXF34",
      "typ": "JWT"
    }
    """.trimIndent()
    assertThat(jsonString).isEqualTo(expectedJsonString)
  }
}