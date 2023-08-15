package ca.ewert.notarytoolkotlin

import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError
import ca.ewert.notarytoolkotlin.authentication.JsonWebToken
import ca.ewert.notarytoolkotlin.i18n.ErrorStringsResource
import ca.ewert.notarytoolkotlin.json.notaryapi.ErrorResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.NewSubmissionRequestJson
import ca.ewert.notarytoolkotlin.json.notaryapi.NewSubmissionResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionListResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionLogUrlResponseJson
import ca.ewert.notarytoolkotlin.json.notaryapi.SubmissionResponseJson
import ca.ewert.notarytoolkotlin.response.AwsUploadData
import ca.ewert.notarytoolkotlin.response.NewSubmissionResponse
import ca.ewert.notarytoolkotlin.response.ResponseMetaData
import ca.ewert.notarytoolkotlin.response.Status
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
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.nio.file.Path
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.io.path.absolutePathString

/** Logger for [NotaryToolClient] class */
private val log = KotlinLogging.logger {}

/**
 * Client used to make requests to Apple's Notary API Web Service. The client can be used to:
 * - Submit software to be notarized [submitSoftware]
 * - Submit and upload the software file to be notarized [submitAndUploadSoftware]
 * - Check the status of a specific notarization submission: [getSubmissionStatus]
 * - Get the url of the submission log for a previous submission [getSubmissionLog]
 * - View the submission log of a previous submission: [retrieveSubmissionLog]
 * - View a history of the latest submissions: [getPreviousSubmissions]
 *
 * In order to use this client you will need an API Key from Apple, which provides
 * the following credentials, that are required to create the client:
 * - Issuer ID
 * - Private Key ID
 * - Private key, in the form of a `.p8` file
 *
 * See [Create API Keys for App Store Connect API](https://developer.apple.com/documentation/appstoreconnectapi/creating_api_keys_for_app_store_connect_api)
 * for more information.
 *
 * @constructor Creates a [NotaryToolClient] that can be used to make requests to Apple's Notary API Web Service. Used for
 * testing with MockWebServer
 * @property privateKeyId The private key ID, provided by Apple.
 * @property issuerId The issuer ID, provided by Apple.
 * @property privateKeyFile The Private Key file `.p8` provided by Apple
 * @property tokenLifetime Lifetime of the token used for Authentication. It should be less than 20 minutes,
 * or request will be rejected by Apple. The default value is **15 minutes**
 * @property baseUrlString The base url of Apple's Notary Web API. The default value is:
 * `https://appstoreconnect.apple.com/notary/v2` This should only be used for testing purposes.
 * @property connectTimeout Sets the default *connect timeout* for the connection. The default value is **10 seconds**
 * @property userAgent Custom `"User-Agent"` to use when sending requests to the Web Service. The default is `notarytool-kotlin/x.y.z`
 * @author Victor Ewert
 */
class NotaryToolClient internal constructor(
  val privateKeyId: String,
  val issuerId: String,
  val privateKeyFile: Path,
  val tokenLifetime: Duration = Duration.of(15, ChronoUnit.MINUTES),
  private val baseUrlString: String,
  val connectTimeout: Duration = Duration.of(10, ChronoUnit.SECONDS),
  val userAgent: String = USER_AGENT_VALUE,
) {

  /**
   * Creates a [NotaryToolClient] that can be used to make requests to Apple's Notary API Web Service.
   *
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

    /**
     * AWS Region to use when uploading.
     * Currently [Region.US_WEST_2]
     */
    private val AWS_REGION: Region = Region.US_WEST_2
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
   * Starts the process of uploading a new version of your software to the notary service.
   * Calls the [Submit Software](https://developer.apple.com/documentation/notaryapi/submit_software) endpoint.
   *
   * Use this method to tell the notary service about a new software submission that you want to make.
   * Do this when you want to notarize a new version of your software.
   *
   * The service responds with temporary security credentials that you use to submit the
   * software to Amazon S3 and a submission identifier that you use to track the submission's status.
   *
   * After uploading your software, you can use the identifier to ask the notary service for the
   * status of your submission using the [getSubmissionStatus] endpoint. If you lose the identifier,
   * you can get a list of your team's 100 most recent submissions using the
   * [getPreviousSubmissions] method. After notarization completes, use the
   * [getSubmissionLog] to get details about the outcome of notarization. Do this even if notarization
   * succeeds, because the log might contain warnings that you can fix before your next submission.
   *
   * This method only starts a submission, but doesn't upload the file to Amazon S3. Use this
   * if you want to upload the file to Amazon S3 yourself. Use [submitAndUploadSoftware] to submit and
   * upload at the same time.
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
          log.debug { "URL String: $url" }
          try {
            createSubmissionRequest(softwarePath, url, jsonWebToken).flatMap { request: Request ->
              this.httpClient.newCall(request).execute().use { response: Response ->
                log.debug { "Response from ${response.request.url}: $response" }
                val responseMetaData = ResponseMetaData(response = response)
                log.debug { "Response body: ${responseMetaData.rawContents}" }

                if (response.isSuccessful) {
                  NewSubmissionResponseJson.create(responseMetaData.rawContents).map { newSubmissionResponseJson ->
                    NewSubmissionResponse(responseMetaData = responseMetaData, jsonResponse = newSubmissionResponseJson)
                  }
                } else {
                  Err(handleUnsuccessfulResponse(responseMetaData = responseMetaData))
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
   * A convenience method that calls [submitSoftware] to retrieve the Amazon S3 security credentials,
   * and then uses those credentials to upload the file to the Amazon S3 server. After uploading your software,
   * you can use the returned [AwsUploadData] to get the [SubmissionId], and use that to query the notary service
   * for the status of your submission using the [getSubmissionStatus] method.
   *
   * @param softwareFile Path to the software file being submitted.
   * @return [AwsUploadData] information about the upload including the submissionId,
   * which can be used to check the status of the submission or a [NotaryToolError]
   */
  fun submitAndUploadSoftware(softwareFile: Path): Result<AwsUploadData, NotaryToolError> {
    return this.submitSoftware(softwareFile).andThen { newSubmissionResponse ->
      this.uploadSoftwareSubmission(
        accessKeyId = newSubmissionResponse.awsAccessKeyId,
        secretAccessKey = newSubmissionResponse.awsSecretAccessKey,
        sessionToken = newSubmissionResponse.awsSessionToken,
        bucketName = newSubmissionResponse.bucket,
        objectKey = newSubmissionResponse.objectKey,
        softwareFile = softwareFile,
      ).andThen { eTag ->
        Ok(AwsUploadData(eTag, newSubmissionResponse.id))
      }
    }
  }

  /**
   * Helper method that creates the Request to be sent to the Notary API.
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
    log.debug { "fileName: $fileName" }
    val sha256: String = calculateSha256(softwarePath)
    log.debug { "SHA-256 of file: $sha256" }

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
  private fun uploadSoftwareSubmission(
    accessKeyId: String,
    secretAccessKey: String,
    sessionToken: String,
    bucketName: String,
    objectKey: String,
    softwareFile: Path,
  ): Result<String?, NotaryToolError.AwsUploadError> {
    val credentials = AwsSessionCredentials.Builder()
      .accessKeyId(accessKeyId)
      .secretAccessKey(secretAccessKey)
      .sessionToken(sessionToken).build()

    val s3Client: S3Client = S3Client.builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(credentials),
      )
      .region(AWS_REGION)
      .build()

    val request: PutObjectRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(objectKey)
      .build()

    return try {
      val response: PutObjectResponse = s3Client.putObject(request, softwareFile)
      log.debug { "AWS S3 Response Tag information: ${response.eTag()}" }
      s3Client.close()
      Ok(response.eTag())
    } catch (exception: Exception) {
      log.warn(exception) { "Error uploading file to AWS S3 Bucket" }
      s3Client.close()
      Err(
        NotaryToolError.AwsUploadError(exceptionMsg = exception.localizedMessage),
      )
    }
  }

  /**
   * Fetch the status of a software notarization submission.
   * Calls the [Get Submission Status](https://developer.apple.com/documentation/notaryapi/get_submission_status)
   * Endpoint.
   *
   * Use this method to fetch the status of a submission request. Supply the identifier that was received in the id
   * field of the response to the [submitSoftware] method.
   * If the identifier is no longer available, you can get a list of the most recent 100 submissions by
   * calling the [getPreviousSubmissions] method.
   *
   * Along with the status of the request, the response indicates the date that you initiated
   * the request and the software name that you provided at that time.
   *
   * @param submissionId The identifier that you receive from the notary service when you use [submitSoftware]
   * to start a new submission.
   * @return The [SubmissionStatusResponse] or a [NotaryToolError]
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
          log.debug { "URL String: $url" }
          val request: Request = Request.Builder()
            .url(url = url)
            .header(name = USER_AGENT_HEADER, value = userAgent)
            .header(name = AUTHORIZATION_HEADER, value = "Bearer ${jsonWebToken.signedToken}")
            .get()
            .build()

          try {
            httpClient.newCall(request).execute().use { response: Response ->
              log.debug { "Response from ${response.request.url}: $response" }
              val responseMetaData = ResponseMetaData(response = response)
              log.debug { "Response body: ${responseMetaData.rawContents}" }

              if (response.isSuccessful) {
                SubmissionResponseJson.create(responseMetaData.rawContents)
                  .map { submissionResponseJson: SubmissionResponseJson ->
                    SubmissionStatusResponse(responseMetaData = responseMetaData, jsonResponse = submissionResponseJson)
                  }
              } else {
                Err(handleUnsuccessfulResponse(responseMetaData = responseMetaData))
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
   * Polls the Notary API Web service, checking on the status of a submission.
   * It calls [getSubmissionStatus] `maxPollCount` number of times, with a delay
   * of [delayFunction] between each request. To allow 'backing off' the delay can be expressed
   * as a function of the current count.
   * 
   * The polling will end when the submission status is
   * one of `Accepted`, `Invalid` or `Rejected`, or when the `maxPollCount` is reached
   *
   * @param submissionId The id of the submission to check the results for.
   * @param maxPollCount The maximum number of times the status should be checked before 'timing out'.
   * @param delayFunction A function used to calculate the delay, based on the current iteration count.
   * @param progressCallback A callback function that will be called after each iteration when
   * the [getSubmissionStatus] request is successful.
   */
  fun pollSubmissionStatus(
    submissionId: SubmissionId,
    maxPollCount: Int,
    delayFunction: (currentPollCount: Int) -> Duration,
    progressCallback: (currentPollCount: Int, submissionStatusResponse: SubmissionStatusResponse) -> Unit = { _, _ -> },
  ): Result<SubmissionStatusResponse, NotaryToolError> {
    for (count in 1..maxPollCount) {
      when (val submissionStatusResult = getSubmissionStatus(submissionId)) {
        is Ok -> {
          val submissionStatusResponse = submissionStatusResult.value
          progressCallback(count, submissionStatusResponse)
          log.debug { "Current status: $submissionStatusResponse.submissionInfo.status" }
          when (submissionStatusResponse.submissionInfo.status) {
            Status.ACCEPTED, Status.REJECTED, Status.INVALID -> return submissionStatusResult
            else -> {}
          }
        }

        is Err -> return submissionStatusResult
      }
      val delay: Duration = delayFunction(count)
      Thread.sleep(delay.toMillis())
    }
    return Err(NotaryToolError.PollingTimeout(maxPollCount))
  }

  /**
   * Fetches details about a single completed notarization.
   * Calls the [Get Submission Log](https://developer.apple.com/documentation/notaryapi/get_submission_log)
   * Endpoint.
   *
   * Use this method to get a URL that you can download a log file from that enumerates any issues
   * found by the notary service. The URL that you receive is temporary, so be sure to use it to immediately
   * fetch the log. If you need the log again in the future, ask for the URL again.
   *
   * The log file that you download contains JSON-formatted data, and might include both errors and warnings.
   * For information about how to deal with common notarization problems,
   * see [Resolving common notarization issues.](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution/resolving_common_notarization_issues)
   *
   * This method only returns the URL for the log file, to retrieve the contents of the file
   * use [retrieveSubmissionLog], or to download the log to a file use [downloadSubmissionLog]
   *
   * @param submissionId The identifier that you receive from the notary service when you use [submitSoftware]
   * to start a new submission.
   * @return A [SubmissionLogUrlResponse] containing the log URL, or a [NotaryToolError]
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
          log.debug { "URL String: $url" }
          val request: Request = Request.Builder()
            .url(url = url)
            .header(name = USER_AGENT_HEADER, value = userAgent)
            .header(name = AUTHORIZATION_HEADER, value = "Bearer ${jsonWebToken.signedToken}")
            .get()
            .build()

          try {
            this.httpClient.newCall(request).execute().use { response: Response ->
              log.debug { "Response from ${response.request.url}: $response" }
              val responseMetaData = ResponseMetaData(response = response)
              log.debug { "Response body: ${responseMetaData.rawContents}" }
              if (response.isSuccessful) {
                SubmissionLogUrlResponseJson.create(responseMetaData.rawContents)
                  .map { submissionLogUrlResponseJson: SubmissionLogUrlResponseJson ->
                    SubmissionLogUrlResponse(
                      responseMetaData = responseMetaData,
                      jsonResponse = submissionLogUrlResponseJson,
                    )
                  }
              } else {
                Err(handleUnsuccessfulResponse(responseMetaData = responseMetaData))
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
   * @param submissionId The identifier that you receive from the notary service when you use [submitSoftware]
   * to start a new submission.
   * @return The submission log contents or a [NotaryToolError]
   */
  fun retrieveSubmissionLog(submissionId: SubmissionId): Result<String, NotaryToolError> {
    return this.getSubmissionLog(submissionId).andThen { submissionLogUrlResponse ->
      val urlString: String = submissionLogUrlResponse.developerLogUrlString
      log.debug { "Using submissionLog URL: $urlString" }
      try {
        val responseUrl: HttpUrl = urlString.toHttpUrl()
        downloadSubmissionLogContents(
          httpClient = this.httpClient,
          developerLogUrl = responseUrl,
          userAgent = this.userAgent,
        )
      } catch (illegalArgumentException: IllegalArgumentException) {
        log.warn(illegalArgumentException) { "Error parsing submission Log URL." }
        val msg: String = ErrorStringsResource.getString("submission.log.invalid.url.error")
        Err(NotaryToolError.SubmissionLogError(msg = msg, exceptionMsg = illegalArgumentException.localizedMessage))
      }
    }
  }

  /**
   * Requests the submission log url from the Notary API Web Service, using [getSubmissionLog],
   * and uses the url to retrieve the submission log and save it to the
   * location specified.
   *
   * @param submissionId The identifier that you received from the Notary API when you use [submitSoftware]
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
        Err(NotaryToolError.SubmissionLogError(msg = msg, exceptionMsg = exception.localizedMessage))
      }
    }
  }

  /**
   * Fetches a list of your team’s previous notarization submissions.
   * Calls the [Get Previous Submissions](https://developer.apple.com/documentation/notaryapi/get_previous_submissions)
   * Endpoint.
   *
   * Use this method to get the list of submissions associated with your team. The response contains a List of values
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
          log.debug { "URL String: $url" }
          val request: Request = Request.Builder()
            .url(url = url)
            .header(name = USER_AGENT_HEADER, value = userAgent)
            .header(name = AUTHORIZATION_HEADER, value = "Bearer ${jsonWebToken.signedToken}")
            .get()
            .build()

          try {
            httpClient.newCall(request = request).execute().use { response: Response ->
              log.debug { "Response from ${response.request.url}: $response" }
              val responseMetaData = ResponseMetaData(response = response)
              log.debug { "Response body: ${responseMetaData.rawContents}" }
              if (response.isSuccessful) {
                SubmissionListResponseJson.create(jsonString = responseMetaData.rawContents)
                  .map { submissionListResponseJson: SubmissionListResponseJson ->
                    SubmissionListResponse(
                      responseMetaData = responseMetaData,
                      jsonResponse = submissionListResponseJson,
                    )
                  }
              } else {
                Err(handleUnsuccessfulResponse(responseMetaData = responseMetaData))
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
 * Handles cases where http response unsuccessful, i.e. not 2xx.
 * It checks various error cases and returns the appropriate error.
 *
 * @param responseMetaData used for checking status codes displaying errors.
 * @return The appropriate [NotaryToolError] subtype.
 */
private fun handleUnsuccessfulResponse(responseMetaData: ResponseMetaData): NotaryToolError {
  return when (responseMetaData.httpStatusCode) {
    401, 403 -> {
      NotaryToolError.UserInputError.AuthenticationError()
    }

    404 -> {
      log.debug { "Content-Type: ${responseMetaData.contentType}" }
      log.debug { "Content-Length: ${responseMetaData.contentLength}" }
      if (isGeneral404(responseMetaData = responseMetaData)) {
        NotaryToolError.HttpError.ClientError4xx(
          msg = ErrorStringsResource.getString("http.400.error"),
          responseMetaData = responseMetaData,
        )
      } else {
        // This is a Notary Error Response, likely incorrect submissionId
        return when (
          val errorResponseJsonResult =
            ErrorResponseJson.create(responseMetaData.rawContents)
        ) {
          is Ok -> {
            // FIXME: Should maybe check that there is at least one error
            NotaryToolError.UserInputError.InvalidSubmissionIdError(errorResponseJsonResult.value.errors[0].detail)
          }

          is Err -> {
            log.warn { errorResponseJsonResult.error }
            errorResponseJsonResult.error
          }
        }
      }
    }

    in 400..499 -> {
      NotaryToolError.HttpError.ClientError4xx(
        msg = ErrorStringsResource.getString("http.400.error"),
        responseMetaData = responseMetaData,
      )
    }

    in 500..599 -> {
      NotaryToolError.HttpError.ServerError5xx(
        msg = ErrorStringsResource.getString("http.500.error"),
        responseMetaData = responseMetaData,
      )
    }

    else -> {
      NotaryToolError.HttpError.OtherError(
        msg = ErrorStringsResource.getString("http.other.error"),
        responseMetaData = responseMetaData,
      )
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
  log.debug { "Found content type: $contentType" }
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
private fun downloadSubmissionLogContents(
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
      log.debug { "Response from ${response.request.url}: $response" }
      log.debug { "Response status code: ${response.code}" }
      val responseMetaData = ResponseMetaData(response = response)
      if (response.isSuccessful) {
        Ok(responseMetaData.rawContents ?: "")
      } else {
        when (response.code) {
          in 400..499 -> {
            Err(
              NotaryToolError.HttpError.ClientError4xx(
                msg = ErrorStringsResource.getString("submission.log.http.400.error"),
                responseMetaData = responseMetaData,
              ),
            )
          }

          in 500..599 -> {
            Err(
              NotaryToolError.HttpError.ServerError5xx(
                msg = ErrorStringsResource.getString("submission.log.http.500.error"),
                responseMetaData = responseMetaData,
              ),
            )
          }

          else -> {
            Err(
              NotaryToolError.HttpError.OtherError(
                msg = ErrorStringsResource.getString("submission.log.http.other.error"),
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
