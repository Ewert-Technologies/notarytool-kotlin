package ca.ewert.notarytoolkotlin

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.asByteStream
import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError
import ca.ewert.notarytoolkotlin.authentication.JsonWebToken
import ca.ewert.notarytoolkotlin.i18n.ErrorStringsResource
import ca.ewert.notarytoolkotlin.json.notaryapi.ErrorResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.NewSubmissionRequestJson
import ca.ewert.notarytoolkotlin.json.notaryapi.NewSubmissionResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionListResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionLogUrlResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionResponseJson
import ca.ewert.notarytoolkotlin.response.NewSubmissionResponse
import ca.ewert.notarytoolkotlin.response.ResponseMetaData
import ca.ewert.notarytoolkotlin.response.SubmissionId
import ca.ewert.notarytoolkotlin.response.SubmissionListResponse
import ca.ewert.notarytoolkotlin.response.SubmissionLogUrlResponse
import ca.ewert.notarytoolkotlin.response.SubmissionStatusResponse
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import java.nio.file.Path
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.io.path.absolutePathString

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
    internal const val USER_AGENT_HEADER = "User-Agent"

    /**
     * Default value for the User-Agent, i.e. `notarytool-kotlin/0.1.0`
     */
    private const val USER_AGENT_VALUE = "notarytool-kotlin/0.1.0"

    /**
     * Constant for the `Authorization` header.
     */
    private const val AUTHORIZATION_HEADER = "Authorization"

    /**
     * Constant for media type of: `application/json`
     */
    private val MEDIA_TYPE_JSON: MediaType = "application/json".toMediaType()
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
   * Start the process of uploading a new version of your software to the notary service.
   *
   * Use this function to tell the notary service about a new software submission that you want to make.
   * Do this when you want to notarize a new version of your software.
   *
   * The service responds with temporary security credentials that you use to submit the
   * software to Amazon S3 and a submission identifier that you use to track the submission’s status.
   *
   * After uploading your software, you can use the identifier to ask the notary service for the
   * status of your submission using the Get Submission Status endpoint. If you lose the identifier,
   * you can get a list of your team’s 100 most recent submissions using the
   * [getPreviousSubmissions] method. After notarization completes, use the
   * [getSubmissionLog] to get details about the outcome of notarization. Do this even if notarization
   * succeeds, because the log might contain warnings that you can fix before your next submission.
   *
   * @param softwarePath Path to the software file being submitted.
   * @return The [NewSubmissionResponse] or a [NotaryToolError] if there is an error.
   */
  fun submitSoftware(softwarePath: Path): Result<NewSubmissionResponse, NotaryToolError> {
    return when (this.jsonWebTokenResult) {
      is Ok -> {
        val jsonWebToken: JsonWebToken = jsonWebTokenResult.value
        if (jsonWebToken.isExpired) {
          jsonWebToken.updateWebToken()
        }
        if (baseUrl != null) {
          val url: HttpUrl = baseUrl.newBuilder().addPathSegment(ENDPOINT_STRING).build()
          log.info { "URL String: $url" }
          try {
            createSubmissionRequest(softwarePath, url, jsonWebToken).flatMap { request: Request ->
              this.httpClient.newCall(request).execute().use { response: Response ->
                log.info { "Response from ${response.request.url}: $response" }
                val responseMetaData = ResponseMetaData(response = response)
                log.info { "Response body: ${responseMetaData.rawContents}" }

                if (response.isSuccessful) {
                  NewSubmissionResponseJson.create(responseMetaData.rawContents).map { newSubmissionResponseJson ->
                    NewSubmissionResponse(responseMetaData = responseMetaData, jsonResponse = newSubmissionResponseJson)
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
                            responseMetaData = responseMetaData,
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
                          responseMetaData = responseMetaData,
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
                          responseMetaData = responseMetaData,
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
                          responseMetaData = responseMetaData,
                        ),
                      )
                    }
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
   * Convenience method that calls [submitSoftware] to retrieve the AWS required Credentials and then
   * uses those credentials to upload the file to the AWS Servers. After uploading your software,
   * you can use the returned submissionId to ask the notary service for the status of your
   * submission using the [getSubmissionStatus] function.
   *
   * @param softwareFile Path to the software file being submitted.
   * @return The submissionId, which can be used to check the status of the submission.
   */
  suspend fun submitAndUploadSoftware(softwareFile: Path): Result<SubmissionId, NotaryToolError> {
    return this.submitSoftware(softwareFile).andThen { newSubmissionResponse ->
      this.uploadSoftwareSubmission(
        accessKeyId = newSubmissionResponse.awsAccessKeyId,
        secretAccessKey = newSubmissionResponse.awsSecretAccessKey,
        sessionToken = newSubmissionResponse.awsSessionToken,
        bucketName = newSubmissionResponse.bucket,
        objectKey = newSubmissionResponse.objectKey,
        softwareFile = softwareFile,
      )
      Ok(newSubmissionResponse.id)
    }
  }

  /**
   * Helper function that creates the Request to be sent to the Notary API.
   *
   * @param softwarePath Path to the software file being submitted.
   * @param url Url to send the Request to
   * @param jsonWebToken JSON Web Token, used for Authentication of the Request.
   * @return The [Request] object or an [NotaryToolError.JsonCreateError]
   */
  private fun createSubmissionRequest(
    softwarePath: Path,
    url: HttpUrl,
    jsonWebToken: JsonWebToken,
  ): Result<Request, NotaryToolError.JsonCreateError> {
    return this.createSubmissionRequestBody(softwarePath).andThen { requestBody ->
      Ok(
        Request.Builder()
          .url(url = url)
          .header(name = USER_AGENT_HEADER, value = userAgent)
          .header(name = AUTHORIZATION_HEADER, value = "Bearer ${jsonWebToken.signedToken}")
          .post(requestBody)
          .build(),
      )
    }
  }

  /**
   * Creates the body of the request by calculating the SHA-256 hash of the file, using the information
   * to create a json object, then converting the json object to String and then using the String to create
   * the request body.
   *
   * @param softwarePath Path to the software file being submitted.
   * @return A [RequestBody] or an [NotaryToolError.JsonCreateError]
   */
  private fun createSubmissionRequestBody(softwarePath: Path): Result<RequestBody, NotaryToolError.JsonCreateError> {
    val fileName: String = softwarePath.fileName.toString()
    log.info { "fileName: $fileName" }
    val sha256: String = calculateSha256(softwarePath)
    log.info { "SHA-256 of file: $sha256" }

    val newSubmissionRequestJson =
      NewSubmissionRequestJson(notifications = emptyList(), sha256 = sha256, submissionName = fileName)

    return NewSubmissionRequestJson.toJsonString(newSubmissionRequestJson).andThen { jsonString: String ->
      Ok(jsonString.toRequestBody(MEDIA_TYPE_JSON))
    }
  }

  /**
   * Uploads the software file to AWS S3 using the credential provided by the [NewSubmissionResponse]
   *
   * @param accessKeyId = AWS Access Key id.
   * @param secretAccessKey = AWS Secret Access Key.
   * @param sessionToken = AWS Session Token.
   * @param bucketName = AWS S3 Bucket name to upload software file to.
   * @param objectKey = The object key that identifies your software within the bucket
   * @param softwareFile = The software file to upload
   */
  private suspend fun uploadSoftwareSubmission(
    accessKeyId: String,
    secretAccessKey: String,
    sessionToken: String,
    bucketName: String,
    objectKey: String,
    softwareFile: Path,
  ): Result<String?, NotaryToolError.AwsUploadError> {
    val credentials =
      Credentials(accessKeyId = accessKeyId, secretAccessKey = secretAccessKey, sessionToken = sessionToken)

    val s3Client = S3Client {
      region = "us-west-2"
      credentialsProvider = StaticCredentialsProvider(credentials)
    }

    val request = PutObjectRequest {
      bucket = bucketName
      key = objectKey
      body = softwareFile.asByteStream()
    }

    s3Client.use { client: S3Client ->
      try {
        val response: PutObjectResponse = client.putObject(request)
        log.info { "AWS S3 Response Tag information: ${response.eTag}" }
        return Ok(response.eTag)
      } catch (exception: Exception) {
        log.warn(exception) { "Error uploading file to AWS S3 Bucket" }
        return Err(NotaryToolError.AwsUploadError("", exception))
      }
    }
  }

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
              val responseMetaData = ResponseMetaData(response = response)
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
                          responseMetaData = responseMetaData,
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
                        responseMetaData = responseMetaData,
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
                        responseMetaData = responseMetaData,
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
                        responseMetaData = responseMetaData,
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
              val responseMetaData = ResponseMetaData(response = response)
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
                          responseMetaData = responseMetaData,
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
                        responseMetaData = responseMetaData,
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
                        responseMetaData = responseMetaData,
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
                        responseMetaData = responseMetaData,
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
   * Requests the submission log url from the Notary API Web Service using [getSubmissionLog],
   * and uses the url to retrieve the submission log as a String.
   *
   * @param submissionId The identifier that you receive from the notary service when you post to `Submit Software`
   * to start a new submission.
   */
  fun retrieveSubmissionLog(submissionId: SubmissionId): Result<String, NotaryToolError> {
    return this.getSubmissionLog(submissionId).andThen { submissionLogUrlResponse ->
      val urlString: String = submissionLogUrlResponse.developerLogUrlString
      log.info { "Using submissionLog URL: $urlString" }
      try {
        val responseUrl: HttpUrl = urlString.toHttpUrl()
        downloadSubmissionLog(httpClient = this.httpClient, developerLogUrl = responseUrl, userAgent = this.userAgent)
      } catch (illegalArgumentException: IllegalArgumentException) {
        val msg: String = ErrorStringsResource.getString("submission.log.invalid.url.error")
          .format(illegalArgumentException.localizedMessage)
        log.warn(illegalArgumentException) { "Error parsing submission Log URL." }
        Err(NotaryToolError.SubmissionLogError(msg = msg))
      }
    }
  }

  /**
   * Requests the submission log url from the Notary API Web Service using [getSubmissionLog],
   * and uses the url to retrieve the submission log and saves it to the
   * location indicated.
   *
   * @param submissionId The identifier that you receive from the notary service when you post to `Submit Software`
   * to start a new submission.
   * @param location Location to download the submission log to.
   * @return The [Path] the submissionLog was downloaded to, or a [NotaryToolError]
   */
  fun downloadSubmissionLog(submissionId: SubmissionId, location: Path): Result<Path, NotaryToolError> {
    return this.retrieveSubmissionLog(submissionId).andThen { logString: String ->

      try {
        val file = location.absolutePathString().toPath()
        FileSystem.SYSTEM.write(file = file) {
          writeUtf8(logString)
          Ok(file.toNioPath())
        }
      } catch (exception: Exception) {
        log.warn(exception) { "Error downloading Submission Log" }
        val msg: String = ErrorStringsResource.getString("submission.log.download.error")
          .format(exception.localizedMessage)
        Err(NotaryToolError.SubmissionLogError(msg = msg))
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
              val responseMetaData = ResponseMetaData(response = response)
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
                        responseMetaData = responseMetaData,
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
                        responseMetaData = responseMetaData,
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
                        responseMetaData = responseMetaData,
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

/**
 * Determines whether the Response is a General 404, as opposed to a 404 caused by
 * using an incorrect submissionId. It checks if the content-type is `"text/plain"`,
 * or if the content-length is zero, and if so assumes it is a General 404,
 * since the other case would include a json body.
 */
private fun isGeneral404(responseMetaData: ResponseMetaData): Boolean {
  val contentType = responseMetaData.contentType
  log.info { "Found content type: $contentType" }
  val contentLength: Long = responseMetaData.contentLength ?: 0
  return contentType?.contains(other = "text/plain", ignoreCase = true) ?: false || contentLength == 0L
}

/**
 * Downloads the logs for a submission, using the developerLogUrl passed in. The developerLogUrl can
 * be obtained by using [NotaryToolClient.getSubmissionLog]. The Response body is returned 'as is' as a String, which may be
 * empty.
 *
 * @param httpClient The [OkHttpClient] to use for making the download
 * @param developerLogUrl URL that you use to download the logs for a submission
 * @param userAgent The User Agent to use when making the download
 * @return The Submission Log, which is a json String, that contains the log information, or a [NotaryToolError]
 */
private fun downloadSubmissionLog(
  httpClient: OkHttpClient,
  developerLogUrl: HttpUrl,
  userAgent: String,
): Result<String, NotaryToolError> {
  val request: Request = Request.Builder()
    .url(developerLogUrl)
    .header(name = NotaryToolClient.USER_AGENT_HEADER, value = userAgent)
    .get()
    .build()

  return try {
    httpClient.newCall(request = request).execute().use { response: Response ->
      log.info { "Response from ${response.request.url}: $response" }
      log.info { "Response status code: ${response.code}" }
      val responseMetaData = ResponseMetaData(response = response)
      if (response.isSuccessful) {
        Ok(responseMetaData.rawContents ?: "")
      } else {
        when (response.code) {
          in 400..499 -> {
            Err(
              NotaryToolError.HttpError.ClientError4xx(
                msg = ErrorStringsResource.getString("submission.log.http.400.error"),
                httpStatusCode = responseMetaData.httpStatusCode,
                httpStatusMsg = responseMetaData.httpStatusMessage,
                requestUrl = developerLogUrl.toString(),
                contentBody = responseMetaData.rawContents,
                responseMetaData = responseMetaData,
              ),
            )
          }

          in 500..599 -> {
            Err(
              NotaryToolError.HttpError.ServerError5xx(
                msg = ErrorStringsResource.getString("submission.log.http.500.error"),
                httpStatusCode = responseMetaData.httpStatusCode,
                httpStatusMsg = responseMetaData.httpStatusMessage,
                requestUrl = developerLogUrl.toString(),
                contentBody = responseMetaData.rawContents,
                responseMetaData = responseMetaData,
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
                responseMetaData = responseMetaData,
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
 * Calculates the SHA-256 hash of the file.
 *
 * @param softwarePath Path to the software file being submitted.
 * @return The SHA-256 hash of the file as a String
 */
internal fun calculateSha256(softwarePath: Path): String {
  return FileSystem.SYSTEM.source(softwarePath.toString().toPath()).buffer().readByteString().sha256().hex()
}
