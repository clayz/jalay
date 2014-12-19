package core.common

import play.api.mvc.Result
import core.mvc.CommonStatusCode

/**
 * Base exception class, this class provides default operations for handling exception.
 * All exceptions in this system should extends this class and override the default functions if in need.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
abstract class BaseException(message: String, cause: Option[Throwable], logLevel: Log.Level.Value = Log.Level.ERROR)
  extends Exception(message, cause.getOrElse(null)) {
  /**
   * Exception handling logic, print exception info and execute customized handler.
   *
   * @param customizedHandler Customized exception handling logic.
   * @return Any Value returns according to customizedHandler.
   */
  def handleResult(customizedHandler: => Result, userId: Option[Long] = None): Result = {
    cause match {
      case Some(e) => logLevel match {
        case Log.Level.ERROR => Log.error(this.getMessage(userId), e)
        case Log.Level.CRIT => Log.crit(this.getMessage(userId), e)
        case Log.Level.WARN => Log.warn(this.getMessage(userId), e)
        case Log.Level.INFO => Log.info(this.getMessage(userId), e)
        case Log.Level.DEBUG => Log.debug(this.getMessage(userId), e)
      }
      case _ => logLevel match {
        case Log.Level.ERROR => Log.error(this.getMessage(userId), this)
        case Log.Level.CRIT => Log.crit(this.getMessage(userId), this)
        case Log.Level.WARN => Log.warn(this.getMessage(userId))
        case Log.Level.INFO => Log.info(this.getMessage(userId))
        case Log.Level.DEBUG => Log.debug(this.getMessage(userId))
      }
    }

    customizedHandler
  }

  /**
   * Generate message with user Id if exists.
   *
   * @param userId Current login user Id.
   * @return String Message to print into log.
   */
  def getMessage(userId: Option[Long] = None): String = userId match {
    case Some(id) => "[%d] ".format(id) + message
    case _ => message
  }
}

/**
 * Handle all unclassified exceptions in whole system.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class CommonException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this(message: String) = this(message, None)

  def this(cause: Throwable) = this(cause.getMessage, None)

  def this(message: String, cause: Throwable) = this(cause.getMessage, Some(cause))
}

/**
 * Database related exceptions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class DBException(message: String, cause: Option[Throwable], logLevel: Log.Level.Value = Log.Level.ERROR) extends BaseException(message, cause, logLevel) {
  def this(message: String) = this(message, None)

  def this(message: String, logLevel: Log.Level.Value) = this(message, None, logLevel)

  def this(cause: Throwable) = this(cause.getMessage, None)

  def this(message: String, cause: Throwable) = this(cause.getMessage, Some(cause))

  def this(message: String, cause: Throwable, logLevel: Log.Level.Value) = this(cause.getMessage, Some(cause), logLevel)
}

/**
 * API parameters validation related exceptions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class ParamException(message: String, cause: Option[Throwable]) extends BaseException(message, cause, Log.Level.WARN) {
  def this(message: String) = this(message, None)

  def this(message: String, cause: Throwable) = this(message, Some(cause))

  override def handleResult(customizedHandler: => Result, userId: Option[Long] = None): Result = {
    Log.warn(super.getMessage(userId))
    customizedHandler
  }
}

/**
 * API related exceptions, this exception will be used in all subsystems.
 * It contains status code and customized messages for following purpose:
 *
 * 1. Status code - will be used in generate API response data.
 * 2. Message - only print this message in error log, the API invoker will not see it at all.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class APIException(statusCode: (String, String), message: String, cause: Option[Throwable], logLevel: Log.Level.Value = Log.Level.ERROR)
  extends BaseException(message, cause, logLevel) {
  def this() = this(CommonStatusCode.ERROR, CommonStatusCode.ERROR._1, None)

  def this(statusCode: (String, String)) = this(statusCode, statusCode._1, None)

  def this(statusCode: (String, String), message: String) = this(statusCode, message, None)

  def this(statusCode: (String, String), logLevel: Log.Level.Value) = this(statusCode, statusCode._1, None, logLevel)

  def this(statusCode: (String, String), message: String, logLevel: Log.Level.Value) = this(statusCode, message, None, logLevel)

  def this(message: String) = this(CommonStatusCode.ERROR, message, None)

  def this(message: String, cause: Throwable) = this(CommonStatusCode.ERROR, message, Some(cause))
}

/**
 * Page related exception, this exception will be used in all subsystems.
 * It contains customized messages which will be show on error page directly.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class PageException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this() = this(CommonStatusCode.MSG_UNKNOWN_ERROR, None)

  def this(message: String) = this(message, None)

  def this(cause: Throwable) = this(CommonStatusCode.MSG_UNKNOWN_ERROR, Some(cause))

  def this(message: String, cause: Throwable) = this(message, Some(cause))
}

/**
 * Batch related exceptions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class BatchException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this(message: String) = this(message, None)

  def this(cause: Throwable) = this(cause.getMessage, Some(cause))

  def this(message: String, cause: Throwable) = this(cause.getMessage, Some(cause))
}

/**
 * Cache related exceptions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class CacheException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this(message: String) = this(message, None)

  def this(cause: Throwable) = this(cause.getMessage, Some(cause))

  def this(message: String, cause: Throwable) = this(message, Some(cause))
}

/**
 * Data related exceptions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class DataException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this(message: String) = this(message, None)

  def this(cause: Throwable) = this(cause.getMessage, Some(cause))

  def this(message: String, cause: Throwable) = this(message, Some(cause))
}

/**
 * Email related exceptions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class MailException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this(message: String) = this(message, None)

  def this(cause: Throwable) = this(cause.getMessage, Some(cause))

  def this(message: String, cause: Throwable) = this(message, Some(cause))
}

/**
 * HTTP related exceptions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class HttpException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this(message: String) = this(message, None)

  def this(cause: Throwable) = this(cause.getMessage, Some(cause))

  def this(message: String, cause: Throwable) = this(message, Some(cause))
}

/**
 * Specification tests exceptions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class SpecException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this(message: String) = this(message, None)

  def this(cause: Throwable) = this(cause.getMessage, Some(cause))

  def this(message: String, cause: Throwable) = this(cause.getMessage, Some(cause))
}

/**
 * Concurrent exceptions for multiple threads and concurrent control.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class ConcurrentException(message: String, cause: Option[Throwable]) extends BaseException(message, cause) {
  def this(message: String) = this(message, null)
}