[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Static Badge](https://img.shields.io/badge/Status-Beta-orange)
[![Maven Central](https://img.shields.io/maven-central/v/ca.ewert-technologies.notarytoolkotlin/notarytool-kotlin?color=blue)](https://central.sonatype.com/artifact/ca.ewert-technologies.notarytoolkotlin/notarytool-kotlin)
[![javadoc](https://javadoc.io/badge2/ca.ewert-technologies.notarytoolkotlin/notarytool-kotlin/javadoc.svg)](https://javadoc.io/doc/ca.ewert-technologies.notarytoolkotlin/notarytool-kotlin) 

# notarytool-kotlin

## Description

A Kotlin wrapper/library for notarizing applications for macOS,
using [Apple's Notary Web Service REST API](https://developer.apple.com/documentation/notaryapi). This library tries to
follow the Notary API, as closely as possible, mapping methods to the Endpoint calls. It also provides some convenience
methods that groups several Endpoint calls together.

## Project status

This project is in Beta and its API may be unstable.

## Versioning

This project adheres to [Semantic Versioning 2.0.0](https://semver.org/).

## Contributing

This project is not currently accepting any pull requests. If you find a problem, or have a suggestion please
log an [issue](https://github.com/Ewert-Technologies/notarytool-kotlin/issues).

## Installation

This library is available
on [Maven Central](https://central.sonatype.com/artifact/ca.ewert-technologies.notarytoolkotlin/notarytool-kotlin).

### Maven

```xml

<dependency>
  <groupId>
    ca.ewert-technologies.notarytoolkotlin
  </groupId>
  <artifactId>
    notarytool-kotlin
  </artifactId>
  <version>
    0.2.1
  </version>
</dependency>
```

### Gradle

```kotlin
implementation "ca.ewert-technologies.notarytoolkotlin:notarytool-kotlin:0.2.1"
```

## Usage

Please refer to the [notarytool-kotlin API Docs](https://www.javadoc.io/doc/ca.ewert-technologies.notarytoolkotlin/notarytool-kotlin/latest/index.html)
for complete documentation details.

### Pre-Requisites

To be able to notarize an application for macOS, you need to have an Apple Developer account, with access to App Store
Connect. In order to make calls to the Notary API, you first need to create an API Key;
see: [Creating API Keys for App Store Connect API](https://developer.apple.com/documentation/appstoreconnectapi/creating_api_keys_for_app_store_connect_api)
for instructions. After creating an API key, you should have the following information:

| Item           | Description                                                |
|----------------|------------------------------------------------------------|
| Issuer ID      | Your issuer ID from the API Keys page in App Store Connect |
| Private Key ID | Your Private key ID from App Store Connect                 |
| Private Key    | The `.p8` file downloaded when creating the API Key        |

These items are used to generate a [JSON Web Token (JWT)](https://jwt.io/), which is used for authentication when making
calls to the Notary API
(see
[Generating Tokens for API Requests](https://developer.apple.com/documentation/appstoreconnectapi/generating_tokens_for_api_requests)
for more information). Note: the JWT is generated for you automatically by this library.

### NotaryToolClient

All calls to the Notary API are done using the `NotaryToolClient` class. A `NotaryToolClient` object should only be
created once, subsequent calls should use the same instance.

#### Basic creation using a Private Key (`.p8`) file

This example shows creating a `NotaryToolClient` instance using the Authentication information obtained as above, and
using the default configuration.

```kotlin
val notaryToolClient = NotaryToolClient(
  privateKeyId = "<Private Key ID>",
  issuerId = "<Issuer ID here>",
  privateKeyFile = Path.of(
    "path",
    "to",
    "privateKeyFile.p8"
  ),
)
```

#### Basic creation using a Private Key supplier

This example shows creating a `NotaryToolClient` instance using the Authentication information obtained as above, but
using a function that supplies an `ECPrivateKey` based on the Private Key file.

```kotlin
val notaryToolClient = NotaryToolClient(
  privateKeyId = "<Private Key ID>",
  issuerId = "<Issuer ID here>",
  privateKeyProvider = ::privateKeyProvider
)
)

...

/**
 * Creates an ECPrivateKey.
 *
 * @return an ECPrivateKey or a JsonWebTokenError
 */
internal fun privateKeyProvider(): Result<ECPrivateKey, JsonWebTokenError> {
  // Custom code that creates an ECPrivateKey
}
```

#### Custom Parameters

This example shows creating a `NotaryToolClient` using the Authentication information obtained as above, and using
custom parameters.

```kotlin
val notaryToolClient = NotaryToolClient(
  privateKeyId = "<Private Key ID>",
  issuerId = "<Issuer ID here>",
  privateKeyFile = Path.of(
    "path",
    "to",
    "privateKeyFile.p8"
  ),
  tokenLifetime = Duration.of(
    10,
    ChronoUnit.MINUTES
  ),
  connectTimeout = Duration.of(
    15,
    ChronoUnit.SECONDS
  ),
  userAgent = "MyTool/1.0.0"
)
```

- `tokenLifetime` sets how long the JWT is valid for. Apple requires this to be less than 20 minutes. The default is 15
  minutes.
- `connectTimeout` sets the HTTP connection timeout. The default is 10 seconds.
- `userAgent` sets the `"User-Agent"` to use as part of the request. The default value is: `notarytool-kotlin/x.y.z`
  where `x.y.z` is the current version of the library.

### Functions

The `NotaryToolClient` has the following methods:

| Function                    | Notes                                                                                                                                                                                              |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `submitSoftware()`          | Starts the process of uploading a new version of your software to the notary service. Maps to the [Submit Software](https://developer.apple.com/documentation/notaryapi/submit_software) Endpoint. |
| `getSubmissionStatus()`     | Fetches the status of a software notarization submission. Maps to the [Get Submission Status](https://developer.apple.com/documentation/notaryapi/get_submission_status) Endpoint.                 |
| `getSubmissionLog()`        | Fetches details about a single completed notarization. Maps to the [Get Submission Log](https://developer.apple.com/documentation/notaryapi/get_submission_log) Endpoint.                          |
| `getPreviousSubmissions()`  | Fetches a list of your team’s previous notarization submissions. Maps to the [Get Previous Submissions](https://developer.apple.com/documentation/notaryapi/get_previous_submissions) Endpoint.    |
| `submitAndUploadSoftware()` | A convenience method that calls `submitSoftware()` and then uploads the software to the Amazon S3 Server, where the Notary API can access it.                                                      |
| `pollSubmissionStatus()`    | A convenience method that repeatedly calls `getSubmissionStatus()`, until the status is no longer `In Progress`.                                                                                   |
| `retrieveSubmissionLog()`   | A convenience method that calls `getSubmissionLog()` and returns the log as String.                                                                                                                |
| `downloadSubmssionLog()`    | A convenience method that calls `getSubmissionLog()` and downloads the log as file.                                                                                                                |

Please refer to the [notarytool-kotlin API Docs](https://apidocs.ewert-technologies.ca/notarytool-kotlin/current)
for complete documentation details.

### Error Handling

Errors are handled using a Result type, using the [kotlin-result](https://github.com/michaelbull/kotlin-result) library.
The Result type, has two subtypes, `OK<V>`, which represents success, and contains the value to be returned, and
`Err<E>`, which represents failure, and contains an `NotaryToolError` subtype. In general, when a function returns a
Result type, the Result will either contain the success value, or it will contain an error type. See below for
some examples.

### Examples

#### Submit and Upload example with minimal Error handling

This example uses `submitAndUploadSoftware()` to initiate a submission request, and then upload the software, so it
can be notarized. The returned Result contains the submission id, which can later be used to check the notarization
status.

```kotlin
val notaryToolClient = NotaryToolClient(
  privateKeyId = "<Private Key ID>",
  issuerId = "<Issuer ID here>",
  privateKeyFile = Path.of(
    "path",
    "to",
    "privateKeyFile.p8"
  ),
)

val softwareFile: Path = Path.of("softwareApp.dmg")

val result: Result<AwsUploadData, NotaryToolError> = notaryToolClient.submitAndUploadSoftware(softwareFile)

result.onSuccess { awsUploadData ->
  println("Uploaded file: ${softwareFile.fileName}, and received submissionId: ${awsUploadData.submissionId}")
}

result.onFailure { notaryToolError ->
  println(notaryToolError.longMsg)
}

```

This would return output similar to:

```text
Uploaded file: softwareApp.dmg, and received submissionId: 6253220d-ee59-4682-8d23-9a4fe87eaagg
```

#### Check submission Status

This example uses `getSubmissionStatus()`, to check the status of a previous submission.

```kotlin
val submissionIdResult: Result<SubmissionId, NotaryToolError.UserInputError.MalformedSubmissionIdError> =
  SubmissionId.of("6253220d-ee59-4682-8d23-9a4fe87eaagg")

submissionIdResult.onSuccess { submissionId ->
  val notaryToolClient = NotaryToolClient(
    privateKeyId = "<Private Key ID>",
    issuerId = "<Issuer ID here>",
    privateKeyFile = Path.of(
      "path",
      "to",
      "privateKeyFile.p8"
    ),
  )
  val result: Result<SubmissionStatusResponse, NotaryToolError> = notaryToolClient.getSubmissionStatus(submissionId)
  result.onSuccess { submissionStatusResponse ->
    val statusInfo = submissionStatusResponse.submissionInfo
    println("Status for submission $submissionId (${statusInfo.name}): ${statusInfo.status}")
  }

  result.onFailure { notaryToolError ->
    println(notaryToolError.longMsg)
  }
}

submissionIdResult.onFailure { malformedSubmissionIdError ->
  println(malformedSubmissionIdError)
}
```

This would return output similar to:

```text
Status for submission 6253220d-ee59-4682-8d23-9a4fe87eaagg (softwareApp.dmg): Accepted
```

#### Other examples

Other Kotlin examples can be found in: [Kotlin examples](lib/src/test/kotlin/ca/ewert/notarytoolkotlin/examples/kotlin).
Examples for use with Java can be found in: [Java examples](lib/src/test/java/ca/ewert/notarytoolkotlin/examples/java).

## Support

For any issues or suggestions please
use [GitHub Issues](https://github.com/Ewert-Technologies/notarytool-kotlin/issues).

## Third-Party Dependencies

This project uses the following runtime and test dependencies.

- [koltin-result](https://github.com/michaelbull/kotlin-result)
- [okhttp](https://github.com/square/okhttp)
- [java-jwt](https://github.com/auth0/java-jwt)
- [moshi](https://github.com/square/moshi)
- [aws-sdk](https://github.com/aws/aws-sdk-java)
- [kotlin-logging](https://github.com/oshai/kotlin-logging)
- [slf4j](https://github.com/qos-ch/slf4j)
- [logback](https://github.com/qos-ch/logback)
- [commons-lang3](https://github.com/apache/commons-lang)
- [junit5](https://github.com/junit-team/junit5)
- [assertk](https://github.com/willowtreeapps/assertk)
- [mockwebserver](https://github.com/square/okhttp/tree/master/mockwebserver)

See also [licenses.txt](licenses.txt) file.

## License

This project is licensed under the [MIT License](https://mit-license.org/).
