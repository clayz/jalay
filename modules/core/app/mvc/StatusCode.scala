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
  val SUCCESS = ("000000", "")
  val ERROR = ("999999", "System error.")

  // authorization and authentication
  val ACCESS_DENY = ("000001", "Access deny.")
  val PERMISSION_DENY = ("000002", "Permission deny.")
  val TOKEN_INVALID = ("000003", "Invalid token.")

  // API data related
  val PARAMETER_ERROR = ("001001", "Parameter error.")
  val DATA_NOT_FOUND = ("001002", "Data not found.")
  val DATA_SAVE_FAILED = ("001003", "Save data failed.")
  val PAGE_SIZE_TOO_LARGE = ("001004", "Page size too large.")

  // http related
  val UNSUPPORTED_RESPONSE_FORMAT = ("002000", "Unsupported response format.")
  val REMOTE_SERVER_TIMEOUT = ("002001", "Remote server timeout.")

  // others
  val API_NOT_FOUND = ("009000", "API not found.")
}

/**
 * Common status code definition.
 * Only use this class in core module.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object CommonStatusCode extends StatusCode