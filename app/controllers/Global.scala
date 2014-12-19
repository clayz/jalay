package controllers

import scala.concurrent.Future
import play.api.GlobalSettings
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import core.common._
import core.mvc._
import user.common.UserService

/**
 * Application Global object.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Global extends GlobalSettings {
  /**
   * Template for response JSON data.
   */
  private val json = """{"status": "%s", "message": "%s"}"""

  /**
   * 500 - internal server error
   *
   * When an exception occurs in your application, the onError operation will be called.
   * The default is to use the internal framework error page,but at here ,redirect to a
   * error 500 page.
   */
  override def onError(request: RequestHeader, throwable: Throwable) = {
    UserService.getLoginUserId(request) match {
      case Some(userId) => Log.error(s"[$userId] Internal server error.", throwable)
      case _ => Log.error("Internal server error.", throwable)
    }

    if (this.isAPI(request)) {
      Future.successful(InternalServerError(
        json.format(CommonStatusCode.ERROR._1, CommonStatusCode.ERROR._2)
      ).as(Constant.Http.ContentType.JSON.toString))
    } else if (request.uri.startsWith("/admin/")) {
      // TODO add admin error page
      Future.successful(InternalServerError("Unknown System Error."))
    } else {
      Future.successful(InternalServerError(views.html.mvc.error500()))
    }
  }

  /**
   * 404 - page not found error
   *
   * When an request handler not found occurs in your application, the onHandlerNotFound operation will be called.
   * At here ,redirect to a error 404 page.
   */
  override def onHandlerNotFound(request: RequestHeader) = {
    UserService.getLoginUserId(request) match {
      case Some(userId) => Log.warn(s"[$userId] Requested unknown path: ${request.method} ${request.path}")
      case _ => Log.warn(s"Anonymous user requested unknown path: ${request.method} ${request.path}")
    }

    if (this.isAPI(request)) {
      Future.successful(NotFound(
        json.format(CommonStatusCode.API_NOT_FOUND._1, CommonStatusCode.API_NOT_FOUND._2)
      ).as(Constant.Http.ContentType.JSON.toString))
    } else if (this.isAdmin(request)) {
      // TODO add admin page not found handling
      Future.successful(NotFound("Admin page not found."))
    } else {
      Future.successful(NotFound(views.html.mvc.error404()))
    }
  }

  private def isAPI(request: RequestHeader): Boolean = request.uri.contains("/api/")

  private def isAdmin(request: RequestHeader): Boolean = request.uri.startsWith("/admin/")
}
