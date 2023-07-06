package ca.ewert.notarytoolkotlin

import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError
import ca.ewert.notarytoolkotlin.authentication.JsonWebToken
import ca.ewert.notarytoolkotlin.i18n.ErrorStringsResource
import ca.ewert.notarytoolkotlin.json.notaryapi.ErrorResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionListResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionLogUrlResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionResponseJson
import ca.ewert.notarytoolkotlin.response.NotaryApiResponse
import ca.ewert.notarytoolkotlin.response.SubmissionId
import ca.ewert.notarytoolkotlin.response.SubmissionListResponse
import ca.ewert.notarytoolkotlin.response.SubmissionLogUrlResponse
import ca.ewert.notarytoolkotlin.response.SubmissionStatusResponse
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.nio.file.Path
import java.time.Duration
import java.time.temporal.ChronoUnit

/** Logger for [NotaryToolClient] class */
private val log = KotlinLogging.logger {}

/**
 * Client used to make requests to Apple's Notary Web API. The client can be used to:
 * - Submit software to be notarized
 * - Check the status of a specific notarization submission: [getSubmissionStatus]
 * - View a history of submissions: [getPreviousSubmissions]
 *
 * @constructor Creates a [NotaryToolClient] that can be used to make requests to Apple's Notary Web API. Used for
 * testing with MockWebServer
 * @param privateKeyId The private key ID, provided by Apple.
 * @property privateKeyId The private key ID, provided by Apple.
 * @param issuerId The issuer ID, provided by Apple.
 * @property issuerId The issuer ID, provided by Apple.
 * @param privateKeyFile The Private Key file `.p8` provided by Apple
 * @property privateKeyFile The Private Key file `.p8` provided by Apple
 * @param tokenLifetime Lifetime of the token used for Authentication. It should be less than 20 minutes,
 * or request will be rejected by Apple. The default value is **15 minutes**
 * @property tokenLifetime Lifetime of the token used for Authentication. It should be less than 20 minutes,
 *  * or request will be rejected by Apple. The default value is **15 minutes**
 * @param baseUrlString The base url of Apple's Notary Web API. The default value is:
 * `https://appstoreconnect.apple.com/notary/v2`. This should only be used for testing purposes.
 * @property baseUrlString The base url of Apple's Notary Web API. The default value is:
 * `https://appstoreconnect.apple.com/notary/v2` This should only be used for testing purposes.
 * @param connectTimeout Sets the default *connect timeout* for the connection. The default value is **10 seconds**
 * @property connectTimeout Sets the default *connect timeout* for the connection. The default value is **10 seconds**
 * @param userAgent Custom `"User-Agent"` to use when sending requests. The default is `notarytool-kotlin/x.y.z`
 * @property userAgent Custom `"User-Agent"` to use when sending requests. The default is `notarytool-kotlin/x.y.z`
 * @author Victor Ewert
 */
class NotaryToolClient internal constructor(
  private val privateKeyId: String,
  private val issuerId: String,
  private val privateKeyFile: Path,
  private val tokenLifetime: Duration = Duration.of(15, ChronoUnit.MINUTES),
  private val baseUrlString: String,
  private val connectTimeout: Duration = Duration.of(10, ChronoUnit.SECONDS),
  private val userAgent: String = USER_AGENT_VALUE,
) {

  /**
   * Public constructor.
   *
   * @constructor Creates a [NotaryToolClient] that can be used to make requests to Apple's Notary Web API.
   * @param privateKeyId The private key ID, provided by Apple.
   * @param issuerId The issuer ID, provided by Apple.
   * @param privateKeyFile The Private Key file `.p8` provided by Apple
   * @param tokenLifetime Lifetime of the token used for Authentication. It should be less than 20 minutes,
   * or request will be rejected by Apple. The default value is **15 minutes**
   * @param connectTimeout Sets the default *connect timeout* for the connection. The default value is **10 seconds**
   * @param userAgent Custom `"User-Agent"` to use when sending requests. The default is `notarytool-kotlin/x.y.z`
   */
  constructor(
    privateKeyId: String,
    issuerId: String,
    privateKeyFile: Path,
    tokenLifetime: Duration = Duration.of(15, ChronoUnit.MINUTES),
    connectTimeout: Duration = Duration.of(10, ChronoUnit.SECONDS),
    userAgent: String = USER_AGENT_VALUE,
  ) : this(
    privateKeyId = privateKeyId,
    issuerId = issuerId,
    privateKeyFile = privateKeyFile,
    tokenLifetime = tokenLifetime,
    baseUrlString = BASE_URL_STRING,
    connectTimeout = connectTimeout,
    userAgent = userAgent,
  )

  internal companion object {

    /**
     * Constant for the base URL of Apple's notary web client
     */
    private const val BASE_URL_STRING = "https://appstoreconnect.apple.com/notary/v2"

    /**
     * The name of the endpoint to send requests to
     */
    private const val ENDPOINT_STRING = "submissions"

    /**
     * The logs path component, used when making call to `getSubmissionLog`
     */
    private const val LOGS_PATH_SEGMENT = "logs"

    /**
     * Constant for the `User-Agent` header.
     */
    private const val USER_AGENT_HEADER = "User-Agent"

    /**
     * Default value for the User-Agent, i.e. `notarytool-kotlin/0.1.0`
     */
    private const val USER_AGENT_VALUE = "notarytool-kotlin/0.1.0"

    /**
     * Constant for the `Authorization` header.
     */
    private const val AUTHORIZATION_HEADER = "Authorization"
  }

  /**
   * The HttpClient used to make the Requests
   */
  private val httpClient: OkHttpClient = OkHttpClient.Builder().connectTimeout(duration = connectTimeout).build()

  /**
   * The Base Url, to send the Requests to.
   */
  private val baseUrl: HttpUrl? = baseUrlString.toHttpUrlOrNull()

  /**
   * Json Web Token object used for authentication, when sending a Request
   */
  private val jsonWebTokenResult: Result<JsonWebToken, JsonWebTokenError> =
    JsonWebToken.create(
      privateKeyId = privateKeyId,
      issuerId = issuerId,
      privateKeyFile = privateKeyFile,
      tokenLifetime = tokenLifetime,
    )

  /**
   * Fetch the status of a software notarization submission.
   * Calls the [Get Submission Status](https://developer.apple.com/documentation/notaryapi/get_submission_status)
   * Endpoint.
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
  fun getSubmissionStatus(submissionId: SubmissionId): Result<SubmissionStatusResponse, NotaryToolError> {
    return when (this.jsonWebTokenResult) {
      is Ok -> {
        val jsonWebToken: JsonWebToken = jsonWebTokenResult.value
        if (jsonWebToken.isExpired) {
          jsonWebToken.updateWebToken()
        }
        if (baseUrl != null) {
          val url: HttpUrl = baseUrl.newBuilder().addPathSegment(ENDPOINT_STRING)
            .addPathSegment(submissionId.id).build()
          log.info { "URL String: $url" }
          val request: Request = Request.Builder()
            .url(url = url)
            .header(name = USER_AGENT_HEADER, value = userAgent)
            .header(name = AUTHORIZATION_HEADER, value = "Bearer ${jsonWebToken.signedToken}")
            .get()
            .build()

          try {
            httpClient.newCall(request).execute().use { response: Response ->
              log.info { "Response from ${response.request.url}: $response" }
              val responseMetaData = NotaryApiResponse.ResponseMetaData(response = response)
              log.info { "Response body: ${responseMetaData.rawContents}" }

              if (response.isSuccessful) {
                SubmissionResponseJson.create(responseMetaData.rawContents)
                  .map { submissionResponseJson: SubmissionResponseJson ->
                    SubmissionStatusResponse(responseMetaData = responseMetaData, jsonResponse = submissionResponseJson)
                  }
              } else {
                when (response.code) {
                  401, 403 -> {
                    Err(JsonWebTokenError.AuthenticationError(ErrorStringsResource.getString("authentication.error")))
                  }

                  404 -> {
                    log.info { "Content-Type: ${responseMetaData.contentType}" }
                    log.info { "Content-Length: ${responseMetaData.contentLength}" }
                    if (isGeneral404(responseMetaData = responseMetaData)) {
                      Err(
                        NotaryToolError.HttpError.ClientError4xx(
                          msg = ErrorStringsResource.getString("http.400.error"),
                          httpStatusCode = response.code,
                          httpStatusMsg = response.message,
                          requestUrl = response.request.url.toString(),
                          contentBody = responseMetaData.rawContents,
                        ),
                      )
                    } else {
                      // This is a Notary Error Response, likely incorrect submissionId
                      return when (
                        val errorResponseJsonResult =
                          ErrorResponseJson.create(responseMetaData.rawContents)
                      ) {
                        is Ok -> {
                          // FIXME: Should maybe check that there is at least one error
                          Err(NotaryToolError.UserInputError.InvalidSubmissionIdError(errorResponseJsonResult.value.errors[0].detail))
                        }

                        is Err -> {
                          log.warn { errorResponseJsonResult.error }
                          errorResponseJsonResult
                        }
                      }
                    }
                  }

                  in 400..499 -> {
                    Err(
                      NotaryToolError.HttpError.ClientError4xx(
                        msg = ErrorStringsResource.getString("http.400.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }

                  in 500..599 -> {
                    Err(
                      NotaryToolError.HttpError.ServerError5xx(
                        msg = ErrorStringsResource.getString("http.500.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }

                  else -> {
                    Err(
                      NotaryToolError.HttpError.OtherError(
                        msg = ErrorStringsResource.getString("http.other.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }
                }
              }
            }
          } catch (ioException: IOException) {
            log.warn(ioException) { "Connection Issue: ${ioException.localizedMessage}, ${ioException.cause?.localizedMessage}" }
            Err(NotaryToolError.ConnectionError(ioException.localizedMessage))
          }
        } else {
          Err(NotaryToolError.GeneralError(msg = ErrorStringsResource.getString("other.url.null.error")))
        }
      }

      is Err -> jsonWebTokenResult
    }
  }

  /**
   * Fetch details about a single completed notarization.
   * Calls the [Get Submission Log](https://developer.apple.com/documentation/notaryapi/get_submission_log)
   * Endpoint.
   *
   * Use this function to get a URL that you can download a log file from that enumerates any issues
   * found by the notary service. The URL that you receive is temporary, so be sure to use it to immediately
   * fetch the log. If you need the log again in the future, ask for the URL again.
   *
   * The log file that you download contains JSON-formatted data, and might include both errors and warnings.
   * For information about how to deal with common notarization problems,
   * see [Resolving common notarization issues.](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution/resolving_common_notarization_issues)
   *
   * @param submissionId The identifier that you receive from the notary service when you post to `Submit Software`
   * to start a new submission.
   */
  fun getSubmissionLog(submissionId: SubmissionId): Result<SubmissionLogUrlResponse, NotaryToolError> {
    return when (this.jsonWebTokenResult) {
      is Ok -> {
        val jsonWebToken: JsonWebToken = jsonWebTokenResult.value
        if (jsonWebToken.isExpired) {
          jsonWebToken.updateWebToken()
        }

        if (baseUrl != null) {
          val url: HttpUrl = baseUrl.newBuilder().addPathSegment(ENDPOINT_STRING)
            .addPathSegment(submissionId.id).addPathSegment(LOGS_PATH_SEGMENT).build()
          log.info { "URL String: $url" }
          val request: Request = Request.Builder()
            .url(url = url)
            .header(name = USER_AGENT_HEADER, value = userAgent)
            .header(name = AUTHORIZATION_HEADER, value = "Bearer ${jsonWebToken.signedToken}")
            .get()
            .build()

          try {
            this.httpClient.newCall(request).execute().use { response: Response ->
              log.info { "Response from ${response.request.url}: $response" }
              val responseMetaData = NotaryApiResponse.ResponseMetaData(response = response)
              log.info { "Response body: ${responseMetaData.rawContents}" }
              if (response.isSuccessful) {
                SubmissionLogUrlResponseJson.create(responseMetaData.rawContents)
                  .map { submissionLogUrlResponseJson: SubmissionLogUrlResponseJson ->
                    SubmissionLogUrlResponse(
                      responseMetaData = responseMetaData,
                      jsonResponse = submissionLogUrlResponseJson,
                    )
                  }
              } else {
                when (response.code) {
                  401, 403 -> {
                    Err(JsonWebTokenError.AuthenticationError(ErrorStringsResource.getString("authentication.error")))
                  }

                  404 -> {
                    log.info { "Content-Type: ${responseMetaData.contentType}" }
                    log.info { "Content-Length: ${responseMetaData.contentLength}" }
                    if (isGeneral404(responseMetaData = responseMetaData)) {
                      Err(
                        NotaryToolError.HttpError.ClientError4xx(
                          msg = ErrorStringsResource.getString("http.400.error"),
                          httpStatusCode = response.code,
                          httpStatusMsg = response.message,
                          requestUrl = response.request.url.toString(),
                          contentBody = responseMetaData.rawContents,
                        ),
                      )
                    } else {
                      // This is a Notary Error Response, likely incorrect submissionId
                      return when (
                        val errorResponseJsonResult =
                          ErrorResponseJson.create(responseMetaData.rawContents)
                      ) {
                        is Ok -> {
                          // FIXME: Should maybe check that there is at least one error
                          Err(NotaryToolError.UserInputError.InvalidSubmissionIdError(errorResponseJsonResult.value.errors[0].detail))
                        }

                        is Err -> {
                          log.warn { errorResponseJsonResult.error }
                          errorResponseJsonResult
                        }
                      }
                    }
                  }

                  in 400..499 -> {
                    Err(
                      NotaryToolError.HttpError.ClientError4xx(
                        msg = ErrorStringsResource.getString("http.400.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }

                  in 500..599 -> {
                    Err(
                      NotaryToolError.HttpError.ServerError5xx(
                        msg = ErrorStringsResource.getString("http.500.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }

                  else -> {
                    Err(
                      NotaryToolError.HttpError.OtherError(
                        msg = ErrorStringsResource.getString("http.other.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }
                }
              }
            }
          } catch (ioException: IOException) {
            log.warn(ioException) { "Connection Issue: ${ioException.localizedMessage}, ${ioException.cause?.localizedMessage}" }
            Err(NotaryToolError.ConnectionError(ioException.localizedMessage))
          }
        } else {
          Err(NotaryToolError.GeneralError(msg = ErrorStringsResource.getString("other.url.null.error")))
        }
      }

      is Err -> jsonWebTokenResult
    }
  }

  /**
   * Determines whether the Response is a General 404, as opposed to a 404 caused by
   * using an incorrect submissionId. It checks if the content-type is `"text/plain"`,
   * or if the content-length is zero, and if so assumes it is a General 404,
   * since the other case would include a json body.
   */
  private fun isGeneral404(responseMetaData: NotaryApiResponse.ResponseMetaData): Boolean {
    val type: String? = responseMetaData.contentType?.type
    val subtype: String? = responseMetaData.contentType?.subtype
    val contentType = "$type/$subtype"
    log.info { "Found content type: $contentType" }
    val contentLength: Long = responseMetaData.contentLength ?: 0
    return contentType.contains(other = "text/plain", ignoreCase = true) || contentLength == 0L
  }

  /**
   * Downloads the logs for a submission, using the developerLogUrl passed in. The developerLogUrl can
   * be obtained by using [getSubmissionLog]. The Response body is returned 'as is' as a String, which may be
   * empty.
   *
   * @param developerLogUrl URL that you use to download the logs for a submission
   * @return The Submission Log, which is a JSON-encoded file that contains the log information.
   */
  private fun downloadSubmissionLog(developerLogUrl: HttpUrl): Result<String, NotaryToolError> {
    val request: Request = Request.Builder()
      .url(developerLogUrl)
      .header(name = USER_AGENT_HEADER, value = userAgent)
      .get()
      .build()

    return try {
      this.httpClient.newCall(request = request).execute().use { response: Response ->
        log.info { "Response from ${response.request.url}: $response" }
        log.info { "Response status code: ${response.code}" }
        val responseMetaData = NotaryApiResponse.ResponseMetaData(response = response)
        if (response.isSuccessful) {
          Ok(responseMetaData.rawContents ?: "")
        } else {
          when (response.code) {
            in 400..499 -> {
              Err(
                NotaryToolError.HttpError.ClientError4xx(
                  msg = ErrorStringsResource.getString("http.400.error"),
                  httpStatusCode = responseMetaData.httpStatusCode,
                  httpStatusMsg = responseMetaData.httpStatusMessage,
                  requestUrl = developerLogUrl.toString(),
                  contentBody = responseMetaData.rawContents,
                ),
              )
            }

            in 500..599 -> {
              Err(
                NotaryToolError.HttpError.ServerError5xx(
                  msg = ErrorStringsResource.getString("http.500.error"),
                  httpStatusCode = responseMetaData.httpStatusCode,
                  httpStatusMsg = responseMetaData.httpStatusMessage,
                  requestUrl = developerLogUrl.toString(),
                  contentBody = responseMetaData.rawContents,
                ),
              )
            }

            else -> {
              Err(
                NotaryToolError.HttpError.OtherError(
                  msg = ErrorStringsResource.getString("http.other.error"),
                  httpStatusCode = responseMetaData.httpStatusCode,
                  httpStatusMsg = responseMetaData.httpStatusMessage,
                  requestUrl = developerLogUrl.toString(),
                  contentBody = responseMetaData.rawContents,
                ),
              )
            }
          }
        }
      }
    } catch (ioException: IOException) {
      log.warn(ioException) { "Connection Issue: ${ioException.localizedMessage}, ${ioException.cause?.localizedMessage}" }
      Err(NotaryToolError.ConnectionError(ioException.localizedMessage))
    }
  }

  /**
   * Requests the submission log url from the Notary API Web Service using [getSubmissionLog],
   * and uses the url to retrieve the submission log as a String.
   */
  fun retrieveSubmissionLog(submissionId: SubmissionId): Result<String, NotaryToolError> {
    return this.getSubmissionLog(submissionId).andThen { submissionLogUrlResponse ->
      val urlString: String = submissionLogUrlResponse.developerLogUrlString
      log.info { "Using submissionLog URL: $urlString" }
      try {
        val responseUrl: HttpUrl = urlString.toHttpUrl()
        this.downloadSubmissionLog(developerLogUrl = responseUrl)
      } catch (illegalArgumentException: IllegalArgumentException) {
        val msg: String = ErrorStringsResource.getString("other.invalid.submission.log.url.error")
          .format(illegalArgumentException.localizedMessage)
        log.warn(illegalArgumentException) { "Error parsing submission Log URL." }
        Err(NotaryToolError.GeneralError(msg))
      }
    }
  }

  /**
   * Fetch a list of your team’s previous notarization submissions.
   * Calls the [Get Previous Submissions](https://developer.apple.com/documentation/notaryapi/get_previous_submissions)
   * Endpoint.
   *
   * Use this function to get the list of submissions associated with your team. The response contains a List of values
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
    return when (this.jsonWebTokenResult) {
      is Ok -> {
        val jsonWebToken: JsonWebToken = jsonWebTokenResult.value
        if (jsonWebToken.isExpired) {
          jsonWebToken.updateWebToken()
        }
        if (baseUrl != null) {
          val url: HttpUrl = baseUrl.newBuilder().addPathSegment(ENDPOINT_STRING).build()
          log.info { "URL String: $url" }
          val request: Request = Request.Builder()
            .url(url = url)
            .header(name = USER_AGENT_HEADER, value = userAgent)
            .header(name = AUTHORIZATION_HEADER, value = "Bearer ${jsonWebToken.signedToken}")
            .get()
            .build()

          try {
            httpClient.newCall(request = request).execute().use { response: Response ->
              log.info { "Response from ${response.request.url}: $response" }
              val responseMetaData = NotaryApiResponse.ResponseMetaData(response = response)
              log.info { "Response body: ${responseMetaData.rawContents}" }
              if (response.isSuccessful) {
                SubmissionListResponseJson.create(jsonString = responseMetaData.rawContents)
                  .map { submissionListResponseJson: SubmissionListResponseJson ->
                    SubmissionListResponse(
                      responseMetaData = responseMetaData,
                      jsonResponse = submissionListResponseJson,
                    )
                  }
              } else {
                when (response.code) {
                  401, 403 -> {
                    Err(JsonWebTokenError.AuthenticationError(ErrorStringsResource.getString("authentication.error")))
                  }

                  in 400..499 -> {
                    if (response.code == 404) {
                      log.warn { "404 error when sending request to: $url" }
                    }
                    Err(
                      NotaryToolError.HttpError.ClientError4xx(
                        msg = ErrorStringsResource.getString("http.400.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }

                  in 500..599 -> {
                    Err(
                      NotaryToolError.HttpError.ServerError5xx(
                        msg = ErrorStringsResource.getString("http.500.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }

                  else -> {
                    Err(
                      NotaryToolError.HttpError.OtherError(
                        msg = ErrorStringsResource.getString("http.other.error"),
                        httpStatusCode = responseMetaData.httpStatusCode,
                        httpStatusMsg = responseMetaData.httpStatusMessage,
                        requestUrl = url.toString(),
                        contentBody = responseMetaData.rawContents,
                      ),
                    )
                  }
                }
              }
            }
          } catch (ioException: IOException) {
            log.warn(ioException) { "Connection Issue: ${ioException.localizedMessage}, ${ioException.cause?.localizedMessage}" }
            Err(NotaryToolError.ConnectionError(ioException.localizedMessage))
          }
        } else {
          Err(NotaryToolError.GeneralError(msg = ErrorStringsResource.getString("other.url.null.error")))
        }
      }

      is Err -> jsonWebTokenResult
    }
  }
}
