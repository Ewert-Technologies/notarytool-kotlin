package ca.ewert.notarytoolkotlin.response

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val log = KotlinLogging.logger {}

/**
 * Utility functions for testing responses
 *
 * @author vewert
 */

/**
 * Creates a [MockResponse] with a status of 200 and all headers of an expected Response
 */
fun createMockResponse200(body: String): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  log.info { "Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(200)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Content-Type", "application/octet-stream")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Connection", "keep-alive")
    .addHeader("Set-Cookie", "dqsid=; Expires=Thu, 01 Jan 1970 00")
    .setBody(body)
}

/**
 * Creates a [MockResponse] with a status of 401 with headers and body matching Apples response.
 */
fun createMockResponse401(): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  val content = """
    Unauthenticated

    Request ID: TYPN6E62TY76LDJXYZTMLQRK7I.0.0
  """.trimIndent()
  log.info { "Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(401)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Content-Type", "text/plain")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Connection", "close")
    .setBody(content)
}

/**
 * Creates a [MockResponse] with a status of 401 with headers and body matching Apples response.
 */
fun createMockResponse403(): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  val content = """
    Unauthenticated

    Request ID: TYPN6E62TY76LDJXYZTMLQRK7I.0.0
  """.trimIndent()
  log.info { "Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(403)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Content-Type", "text/plain")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Connection", "close")
    .setBody(content)
}

/**
 * Creates a [MockResponse] with a status of 404 with headers and body matching Apples response.
 * Content-Type is set to `"application/octet-stream"`.
 * Used when Notary API sends an error response as json
 */
fun createMockResponse404ErrorResponse(body: String): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  log.info { "Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(404)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Content-Type", "application/octet-stream")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Connection", "close")
    .setBody(body)
}

/**
 * Creates a [MockResponse] with a status of 404 with headers and body matching Apples response.
 * Content-Type is set to `"text/plain"`.
 * Used when Server sends a general 404, with no content
 */
fun createMockResponse404General(): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  log.info { "Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(404)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Content-Type", "text/plain")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Connection", "close")
    .setBody("")
}

/**
 * Creates a [MockResponse] with a status of 500 with headers and body matching Apples response.
 */
fun createMockResponse500(): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  log.info { "Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(500)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Connection", "close")
    .setBody("")
}

/**
 * Creates a [MockResponse] with a connection issue, simulated using a [SocketPolicy]
 */
fun createMockResponseConnectionProblem(socketPolicy: SocketPolicy): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  log.info { "Header Date String: $headerDateString" }
  return MockResponse().setResponseCode(200)
    .addHeader("Server", "daiquiri/3.0.0")
    .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    .addHeader("Date", headerDateString)
    .addHeader("Content-Type", "text/plain")
    .addHeader("Connection", "close")
    .setBody("Some Text")
    .setSocketPolicy(socketPolicy)
}

/**
 * Creates a [MockResponse] for when using a developerLogUrl.
 */
fun createSubmissionLogResponse(): MockResponse {
  val headerDateString = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"))
    .format(NotaryApiResponse.HTTP_DATE_TIME)
  log.info { "Header Date String: $headerDateString" }

  val body: String = """
  {
    "logFormatVersion": 1,
    "jobId": "1234647e-0125-4343-a068-1c5786499827",
    "status": "Accepted",
    "statusSummary": "Ready for distribution",
    "statusCode": 0,
    "archiveFilename": "PWMinder.zip",
    "uploadDate": "2023-06-13T21:33:39.967Z",
    "sha256": "422f7c6b57492b99ec0803599e7e44941477b0a5c3253f5dc88eb90c672fed08",
    "ticketContents": [
      {
        "path": "PWMinder.zip/PWMinder.app",
        "digestAlgorithm": "SHA-256",
        "cdhash": "e29996973e273b355b976cecde3fc112605ed76f",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/MacOS/PWMinder",
        "digestAlgorithm": "SHA-256",
        "cdhash": "e29996973e273b355b976cecde3fc112605ed76f",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/bin/rmiregistry",
        "digestAlgorithm": "SHA-256",
        "cdhash": "466ed6649fd9e3cbb503ee64a27241cdf034d38f",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/bin/jrunscript",
        "digestAlgorithm": "SHA-256",
        "cdhash": "47720dbef29dee2c1b155475f8666729c305f976",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/bin/java",
        "digestAlgorithm": "SHA-256",
        "cdhash": "686d095a0be8481adb12f83e87cfb0e3e13b403f",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/bin/keytool",
        "digestAlgorithm": "SHA-256",
        "cdhash": "aa0cd658a7eca5d69eb6e71d088fb99241fd0d52",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libnet.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "bdddd8782916f96116a6f4ca85faf4e1a0308055",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libnio.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "606d255e39f0ed74c96b783771ca4fa1a42798bd",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libzip.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "264e5d7eec89866d0dc354580a2b7c729bb076a3",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libfreetype.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "5f4af090a9195d81f2f48abb24db21ba52f767d2",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libjli.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "77770112407aed9b96197d7e17593c8872228db5",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libsplashscreen.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "3d6402ca063ff6ac427096491fef2756d2e74258",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libjimage.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "9f984a54b113947968ba75baa30757a6ab13503d",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libosxkrb5.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "063bfb76c055f52088a369e4b5159a7828840827",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libosxui.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "d31ca30c39793421c6e7ae7c3a7a7ee7694eb90f",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/librmi.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "6d43af7a66d78982c335947664cb0117c16f660a",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libdna.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "fbbb4f13dc7e34444a9cb81569de0106a97f2a57",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libawt_lwawt.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "93b6b48e92864a9399fc423a19438b4e1a853645",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libjavajpeg.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "887dee25a06ba7322285e665ce05a7b5b993937e",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libmlib_image.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "0b6ab11ccbe8bce2e3fcb9a0448d13ee3749587e",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libmanagement.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "8abf09712e851541b4b1225603fb3b1c7e6c5c5a",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libjsound.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "1de0a9f2fcc91c034021fca29cf4b1f3fbfcc46c",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libjsig.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "3a2536bdf8f2d4ab839dcb90f2b09d0dbdd354d0",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libprefs.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "f3af29ad69bf69e87e82c91a940cea03a148c585",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libjawt.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "ce091d29ff952336e6f6db77b7471b4e1977a7a3",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libfontmanager.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "80694fb1f3891696772b1fab3e2522ff7010cee4",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/jspawnhelper",
        "digestAlgorithm": "SHA-256",
        "cdhash": "38d5f37352c0e6f55157024ea1aea16f1a49173c",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libosxsecurity.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "3a3e303d5458d8f692917f9542107f1f93f73097",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/liblcms.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "7898da4b4a372475f1db278f52eb157bf94a97c4",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libverify.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "7765958c8945b6b420ac0336bda5c6719545b42f",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libj2gss.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "68eb444ec66e251c6e0af552866d605c0826d3bb",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libjava.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "502617111f37b6d0e7d134891f1a36732f852870",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libawt.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "24b5e606177d6223ce597c18a696f008fd8fe122",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libosx.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "85b3f99c471da3cd82097ecc489ef63b084a0184",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/libosxapp.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "a46e37793b7664fb4afc95519495ee1706a5fa51",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/server/libjvm.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "e1acbc8bd4329b5d467e9984abaf2b634e1e287c",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/Home/lib/server/libjsig.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "3a2536bdf8f2d4ab839dcb90f2b09d0dbdd354d0",
        "arch": "arm64"
      },
      {
        "path": "PWMinder.zip/PWMinder.app/Contents/runtime/Contents/MacOS/libjli.dylib",
        "digestAlgorithm": "SHA-256",
        "cdhash": "36a40a5d442597f17d1526065980e67890ac9c43",
        "arch": "arm64"
      }
    ],
    "issues": null
  }
  """.trimIndent()

  return MockResponse().setResponseCode(200)
    .addHeader("Server", "AmazonS3")
    .addHeader("x-amz-id-2", "/XXfaAyCapFSd1l5abRelbnuRc2HAWKFm6I3xemkt8mYbHyO4uGioOzqJEph1tROq1sbaATaEZQ=")
    .addHeader("x-amz-request-id", "VGEJ4DZQDB74N7CJ")
    .addHeader("x-amz-server-side-encryption", "AES256")
    .addHeader("ETag", "e2c4e4e914c0f05ea9166d8fda614d3f")
    .addHeader("Accept-Ranges", "bytes")
    .addHeader("Date", headerDateString)
    .addHeader("Content-Type", "binary/octet-stream")
    .setBody(body)
}
