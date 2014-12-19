package core.common

import play.api.Logger

/**
 * Print system log into log file.
 * Please always use this class instead of using Play's Logger directly.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Log {
  private val logger = Logger("Jalay")

  def isDebugEnabled: Boolean = Logger.isDebugEnabled

  def info(msg: String) = logger.info(msg)

  def info(msg: String, throwable: Throwable) = logger.info(msg, throwable)

  def debug(msg: String) = logger.debug(msg)

  def debug(msg: String, throwable: Throwable) = logger.debug(msg, throwable)

  def warn(msg: String) = logger.warn(msg)

  def warn(msg: String, throwable: Throwable) = logger.warn(msg, throwable)

  def error(msg: String) = logger.error(msg)

  def error(msg: String, throwable: Throwable) = logger.error(msg, throwable)

  def crit(msg: String) = logger.error(s"[CRIT] $msg")

  def crit(msg: String, throwable: Throwable) = logger.error(s"[CRIT] $msg", throwable)

  // private def getCaller: String = Reflection.getCallerClass(8).getSimpleName

  /**
   * Supported log levels in core module.
   */
  object Level extends Enumeration {
    val DEBUG = Value(1)
    val WARN = Value(2)
    val INFO = Value(3)
    val ERROR = Value(4)
    val CRIT = Value(5)
  }

}

object FrontLog {
  private val logger = Logger("Front")

  def info(msg: String) = logger.info(msg)

  def debug(msg: String) = logger.debug(msg)

  def warn(msg: String) = logger.warn(msg)

  def error(msg: String) = logger.error(msg)
}