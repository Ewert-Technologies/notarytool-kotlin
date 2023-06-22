package ca.ewert.notarytoolkotlin

import ca.ewert.notarytoolkotlin.authentication.JsonWebToken
import ca.ewert.notarytoolkotlin.errors.JsonWebTokenError
import ca.ewert.notarytoolkotlin.errors.NotaryToolError
import ca.ewert.notarytoolkotlin.http.json.notaryapi.SubmissionListResponseJson
import ca.ewert.notarytoolkotlin.http.response.NotaryApiResponse
import ca.ewert.notarytoolkotlin.http.response.SubmissionListResponse
import com.github.michaelbull.result.*
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.nio.file.Path
import java.time.Duration
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * Client used to make requests to Apple's Notary Web API.
 *
 * @property privateKeyId Apple private key ID
 * @property issuerId Apple Issuer ID
 * @property privateKeyFile Private Key file `.p8` provided by Apple
 * @property tokenLifetime Lifetime of the token, should be less than 20 minutes or request will be rejected by Apple.
 * The default value is **15 minutes**
 * @property baseUrlString The base url of Apple's Notary Web API. The default value is:
 * `https://appstoreconnect.apple.com/notary/v2`
 * @property connectTimeout Sets the default connect timeout for the connection. The default value is **10 seconds**
 * @author vewert
 */
class NotaryToolClient(
  private val privateKeyId: String,
  private val issuerId: String,
  private val privateKeyFile: Path,
  private val tokenLifetime: Duration = Duration.of(15, ChronoUnit.MINUTES),
  private val baseUrlString: String = BASE_URL_STRING,
  private val connectTimeout: Duration = Duration.of(10, ChronoUnit.SECONDS)
) {

  companion object {
    private const val BASE_URL_STRING = "https://appstoreconnect.apple.com/notary/v2"

    private const val ENDPOINT_STRING = "submissionss"

    private const val USER_AGENT_VALUE = "notarytool-kotlin/0.1.0"
  }

  private val httpClient: OkHttpClient = OkHttpClient.Builder().connectTimeout(connectTimeout).build()

  private val baseUrl: HttpUrl? = baseUrlString.toHttpUrlOrNull()

  private val jsonWebTokenResult: Result<JsonWebToken, JsonWebTokenError> =
    JsonWebToken.create(privateKeyId, issuerId, privateKeyFile, tokenLifetime)


  /**
   * Fetch the status of a software notarization submission.
   * Calls the [Get Submission Status](https://developer.apple.com/documentation/notaryapi/get_submission_status)
   *
   * Use this function to fetch the status of a submission request. Supply the identifier that was received in the id
   * field of the response to the Submit Software function.
   * If the identifier is no longer available, you can get a list of the most recent 100 submissions by
   * calling the [getPreviousSubmissions] function.
   *
   * Along with the status of the request, the response indicates the date that you initiated
   * the request and the software name that you provided at that time.
   *
   * @param submissionId The identifier that you receive from the notary service when you post to `Submit Software`
   * to start a new submission.
   */
  fun getSubmissionStatus(submissionId: String) {

  }

  /**
   * Fetch a list of your team’s previous notarization submissions.
   * Calls the [Get Previous Submissions](https://developer.apple.com/documentation/notaryapi/get_previous_submissions)
   *
   * Use this function to get the list of submissions associated with your team. The response contains List of values
   * that include the unique identifier for the submission, the date the submission was initiated,
   * the name of the associated file that was uploaded, and the status of the submission.
   * The response returns information about only the 100 most recent submissions.
   *
   * If you need information about just one submission, and you have the associated identifier,
   * use [getSubmissionStatus] instead.
   *
   * @return A [SubmissionListResponse] or a [NotaryToolError]
   */
  fun getPreviousSubmissions(): Result<SubmissionListResponse, NotaryToolError> {
    return when (jsonWebTokenResult) {
      is Ok -> {
        val jsonWebToken = jsonWebTokenResult.value
        if (jsonWebToken.isExpired) {
          jsonWebToken.updateWebToken()
        }
        if (baseUrl != null) {
          val url = baseUrl.newBuilder().addPathSegment(ENDPOINT_STRING).build()
          log.info { "URL String: $url" }
          val request: Request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT_VALUE)
            .header("Authorization", "Bearer ${jsonWebToken.signedToken}")
            .get()
            .build()

          httpClient.newCall(request).execute().use { response: Response ->
            log.info { "Response from ${response.request.url}: $response" }
            val responseMetaData = NotaryApiResponse.ResponseMetaData(response = response)
            log.info { "Response body: ${responseMetaData.rawContents}" }
            if (response.isSuccessful) {
              val submissionListResponseJsonResult = SubmissionListResponseJson.create(responseMetaData.rawContents)
              submissionListResponseJsonResult.map { submissionListResponseJson ->
                SubmissionListResponse(responseMetaData, submissionListResponseJson)
              }
            } else {
              if (response.code == 404) {
                log.warn { "404 error when sending request to: $url" }
              }
              Err(
                NotaryToolError.HttpError(
                  "Response was unsuccessful",
                  response.code,
                  response.message,
                  url.toString()
                )
              )
            }


          }
        } else {
          Err(NotaryToolError.GeneralError("URL Path was null"))
        }
      }

      is Err -> Err(NotaryToolError.GeneralError("URL Path was null"))
    }
  }
}