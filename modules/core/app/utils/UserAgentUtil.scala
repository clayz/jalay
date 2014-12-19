package core.utils

import play.api.mvc._
import core.common.HttpException

/**
 * HTTP User-Agent utility.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object UserAgentUtil {
  /**
   * Check whether current request is sent from smart phone.
   *
   * @param request HTTP request header.
   * @return Boolean Whether sent from smart phone.
   */
  def isSP(implicit request: RequestHeader): Boolean = """(?i)(Android|iPhone|iPad)""".r.findFirstIn(this.getUserAgent(request)).isDefined

  /**
   * Check whether current request is sent from desktop.
   *
   * @param request HTTP request header.
   * @return Boolean Whether sent from desktop.
   */
  def isPC(implicit request: RequestHeader): Boolean = !this.isSP

  /**
   * Check whether current request is send from iPhone or iPad.
   */
  def isIOS(implicit request: RequestHeader): Boolean = """(?i)(iPhone|iPad)""".r.findFirstIn(this.getUserAgent(request)).isDefined

  /**
   * Check whether current request is send from Android.
   */
  def isAndroid(implicit request: RequestHeader): Boolean = """(?i)(Android)""".r.findFirstIn(this.getUserAgent(request)).isDefined

  /**
   * Get user agent value from HTTP request header.
   *
   * @param request HTTP request header.
   * @return String User agent value.
   */
  private def getUserAgent(request: RequestHeader): String = request.headers.get("User-Agent") match {
    case Some(agent) => agent
    case _ => throw new HttpException("Cannot find User-Agent in HTTP request header.")
  }
}