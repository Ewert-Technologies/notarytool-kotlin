# notarytool-kotlin

## Description

A wrapper/library for notarizing applications
using [Apples Notary Web Service REST API](https://developer.apple.com/documentation/notaryapi). This library tries to
following the Notary API, as closely as possible, mapping methods to the Endpoints calls. It also provides some
convenience methods that groups several Endpoint calls together.

## Badges

On some READMEs, you may see small images that convey metadata, such as whether or not all the tests are passing for the
project. You can use Shields to add some to your README. Many services also have instructions for adding a badge.

## Visuals

Depending on what you are making, it can be a good idea to include screenshots or even a video (you'll frequently see
GIFs rather than actual videos). Tools like ttygif can help, but check out Asciinema for a more sophisticated method.

## Installation

This library is available on Maven Central.

### Maven

```xml

<dependency>
  <groupId>ca.ewert.notarytoolkotlin</groupId>
  <artifactId>notarytool-kotlin</artifactId>
  <version>0.1.0</version>
</dependency>
```

### Gradle

```kotlin
implementation "ca.ewert.notarytoolkotlin:notarytool-kotlin:0.1.0"
```

## Usage

### Pre-Requisites

In order to notarize an application, you need to have an Apple Developer ID account with access to App Store Connect. In
order to make calls to the Notary API, you first need to create an API Key,
see: [Creating API Keys for App Store Connect API](https://developer.apple.com/documentation/appstoreconnectapi/creating_api_keys_for_app_store_connect_api)
for instructions. After creating an API key, you should have the following information:

| Name           | Description                                                    |
|----------------|----------------------------------------------------------------|
| Issuer ID      | Your issuer ID from the API Keys page in the App Store Connect |
| Private Key ID | Your Private key ID from App Store Connect                     |
| Private Key    | The `.p8` file downloaded when creating the API Key            |

These items are used to generate a JSON Web Token (JWT), used as authentication when making calls to the Notary API see
[Generating Tokens for API Requests](https://developer.apple.com/documentation/appstoreconnectapi/generating_tokens_for_api_requests).
Note: the JWT is created for you automatically by the library.

### NotaryToolClient

All calls to the Notary API are done using the `NotaryToolClient` class. A `NotaryToolClient` object should only be
created once, subsequent calls should use the same instance.

#### Basic creation

This example shows creating a `NotaryToolClient` using the Authentication information obtained as above, and using the
default configuration.

```kotlin
val notaryToolClient = NotaryToolClient(
  privateKeyId = "<Private Key ID>",
  issuerId = "<Issuer ID here>",
  privateKeyFile = Path.of("path", "to", "privateKeyFile.p8"),
)
```

#### Custom Parameters

This example shows creating a `NotaryToolClient` using the Authentication information obtained as above, and using
custom parameters.

```kotlin
val notaryToolClient = NotaryToolClient(
  privateKeyId = "<Private Key ID>",
  issuerId = "<Issuer ID here>",
  privateKeyFile = Path.of("path", "to", "privateKeyFile.p8"),
  tokenLifetime = Duration.of(10, ChronoUnit.MINUTES),
  connectTimeout = Duration.of(15, ChronoUnit.SECONDS),
  userAgent = "MyTool/1.0.0"
)
```

- `tokenLifetime` sets how long the JWT is valid for. Apple requires this to be less than 20 minutes. The default is 15
  minutes.
- `connectTimeout` set the HTTP connection timeout. The default is 10 seconds
- `userAgent` sets the `"User-Agent"` to use as part of the request. The default value is: `notarytool-kotlin/x.y.z`

### Functions

The `NotaryToolClient` has the following functions:

| Function                    | Notes                                                                                                                                                                                         |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `submitSoftware()`          | Start the process of uploading a new version of your software to the notary service. Maps to [Submit Software](https://developer.apple.com/documentation/notaryapi/submit_software) Endpoint. |
| `getSubmissionStatus()`     | Fetch the status of a software notarization submission. Maps to [Get Submission Status](https://developer.apple.com/documentation/notaryapi/get_submission_status) Endpoint.                  |
| `getSubmissionLog()`        | Fetch details about a single completed notarization. Maps to [Get Submission Log](https://developer.apple.com/documentation/notaryapi/get_submission_log) Endpoint.                           |
| `getPreviousSubmissions()`  | Fetch a list of your team’s previous notarization submissions. Maps to [Get Previous Submissions](https://developer.apple.com/documentation/notaryapi/get_previous_submissions) Endpoint.     |
| `submitAndUploadSoftware()` | A convenience method that calls `submitSoftware()` and then uploads the software to the Amazon S3 Server, where the Notary API can access it.                                                 |
| `pollSubmissionStatus()`    | A convenience method that repeated calls `getSubmissionStatus()`, until the status is no longer `In Progress`.                                                                                |
| `retrieveSubmissionLog()`   | A convenience method that calls `getSubmissionLog()` and returns the log as String.                                                                                                           |
| `downloadSubmssionLog()`    | A convenience method that calls `getSubmissionLog()` and downloads the log as file.                                                                                                           |

### Errors

### Examples

#### Submit example with minimal Error handling

## Support

Tell people where they can go to for help. It can be any combination of an issue tracker, a chat room, an email address,
etc.

## Roadmap

If you have ideas for releases in the future, it is a good idea to list them in the README.

## Contributing

State if you are open to contributions and what your requirements are for accepting them.

For people who want to make changes to your project, it's helpful to have some documentation on how to get started.
Perhaps there is a script that they should run or some environment variables that they need to set. Make these steps
explicit. These instructions could also be useful to your future self.

You can also document commands to lint the code or run tests. These steps help to ensure high code quality and reduce
the likelihood that the changes inadvertently break something. Having instructions for running tests is especially
helpful if it requires external setup, such as starting a Selenium server for testing in a browser.

## Authors and acknowledgment

Show your appreciation to those who have contributed to the project.

## License

For open source projects, say how it is licensed.

## Project status

If you have run out of energy or time for your project, put a note at the top of the README saying that development has
slowed down or stopped completely. Someone may choose to fork your project or volunteer to step in as a maintainer or
owner, allowing your project to keep going. You can also make an explicit request for maintainers.
