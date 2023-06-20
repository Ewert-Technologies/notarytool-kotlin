package ca.ewert.notarytoolkotlin.errors

/**
 * Parent class of all Error types
 *
 * @author vewert
 */
sealed interface NotaryToolError {
  val msg: String
}

sealed interface JsonWebTokenError : NotaryToolError {
  class PrivateKeyNotFound(override val msg: String) : JsonWebTokenError
  class TokenCreationError(override val msg: String) : JsonWebTokenError
}

sealed class JsonParseError(override val msg: String) : NotaryToolError

sealed class HttpError(override  val msg: String, val httpStatusCode: Int, val httpStatusMsg: String) : NotaryToolError