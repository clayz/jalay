package core.mvc

/**
 * Common status code definition for all subsystems.
 * All status code classes in subsystem should extends from this class.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
trait StatusCode {
  /**
   * Common status code
   */
  lazy val SUCCESS = ("000000", MSG_SUCCESS)
  lazy val ERROR = ("999999", MSG_UNKNOWN_ERROR)

  // authorization and authentication
  lazy val ACCESS_DENY = ("000001", MSG_ACCESS_DENY)
  lazy val PERMISSION_DENY = ("000002", MSG_PERMISSION_DENY)
  lazy val TOKEN_INVALID = ("000003", MSG_TOKEN_INVALID)

  // API data related
  lazy val PARAMETER_ERROR = ("001001", MSG_PARAMETER_ERROR)
  lazy val DATA_NOT_FOUND = ("001002", MSG_DATA_NOT_FOUNT)
  lazy val DATA_SAVE_FAILED = ("001003", MSG_DATA_SAVE_FAILED)
  lazy val PAGE_SIZE_TOO_LARGE = ("001004", MSG_PAGE_SIZE_TOO_LARGE)

  // http related
  lazy val UNSUPPORTED_RESPONSE_FORMAT = ("002000", MSG_UNSUPPORTED_RESPONSE_FORMAT)
  lazy val REMOTE_SERVER_TIMEOUT = ("002001", MSG_REMOTE_SERVER_TIMEOUT)

  // others
  lazy val API_NOT_FOUND = ("009000", MSG_API_NOT_FOUND)

  /**
   * Common messages
   */
  val MSG_SUCCESS = ""
  val MSG_UNKNOWN_ERROR = "System error."
  val MSG_API_NOT_FOUND = "API not found."

  val MSG_ACCESS_DENY = "Access deny."
  val MSG_PERMISSION_DENY = "Permission deny."
  val MSG_TOKEN_INVALID = "Invalid token."

  val MSG_PARAMETER_ERROR = "Parameter error."
  val MSG_DATA_NOT_FOUNT = "Data not found."
  val MSG_DATA_SAVE_FAILED = "Save data failed."
  val MSG_PAGE_SIZE_TOO_LARGE = "Page size too large."

  val MSG_UNSUPPORTED_RESPONSE_FORMAT = "Unsupported response format."
  val MSG_REMOTE_SERVER_TIMEOUT = "Remote server timeout."
}

/**
 * Common status code definition.
 * Only use this class in core module.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object CommonStatusCode extends StatusCode