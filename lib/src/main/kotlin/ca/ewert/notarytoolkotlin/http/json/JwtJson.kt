package ca.ewert.notarytoolkotlin.http.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

/**
 * Data class representing a Jason Web Token header. Used for serializing
 * and de-serializing the raw json. See [JWT Introduction](https://jwt.io/introduction)
 * [App Store Connect API documentation](https://developer.apple.com/documentation/appstoreconnectapi/generating_tokens_for_api_requests)
 *
 * @property alg Encryption Algorithm
 * @property kid Key Identifier
 * @property typ Token Type
 */
data class JwtHeaderJson(
  val alg: String,
  val kid: String,
  val typ: String
) {

  /**
   * String representation of the JwtHeaderJson. The object serialized back into a json String.
   *
   * @param prettyPrint Whether the json should be pretty printed, using an indent of 2
   */
  fun toJsonString(prettyPrint: Boolean = false): String {
    val jsonAdapter: JsonAdapter<JwtHeaderJson>  =
    if (prettyPrint) {
      moshi.adapter(JwtHeaderJson::class.java).indent("  ")
    } else {
      moshi.adapter(JwtHeaderJson::class.java)
    }
    return jsonAdapter.toJson(this)
  }
}

/**
 * Data class representing a Jason Web Token payload. Used for serializing
 * and de-serializing the raw json. See [JWT Introduction](https://jwt.io/introduction)
 * [App Store Connect API documentation](https://developer.apple.com/documentation/appstoreconnectapi/generating_tokens_for_api_requests)
 *
 * @property iss Issuer ID
 * @property iat Issued At Time
 * @property exp Expiration Time
 * @property aud Audience
 * @property scope Token Scope
 */
data class JwtPayloadJson(
  val aud: String,
  val exp: Int,
  val iat: Int,
  val iss: String,
  val scope: List<String>
) {

  /**
   * String representation of the JwtPayloadJson. The object serialized back into a json String
   */
  @OptIn(ExperimentalStdlibApi::class)
  fun toJsonString(prettyPrint: Boolean = false): String {
    val jsonAdapter: JsonAdapter<JwtPayloadJson> =
      if (prettyPrint) {
        moshi.adapter(JwtPayloadJson::class.java).indent(" ")
      } else {
        moshi.adapter(JwtPayloadJson::class.java)
      }

    return jsonAdapter.toJson(this)
  }
}