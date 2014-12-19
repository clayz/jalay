package core.mvc

import core.common._
import play.api.mvc._

/**
 * All page actions in subsystems should extends from this class.
 * It provides basic action functions, user authentication and parameters validation.
 *
 * Implement the userId function attribute in constructor if you want to use PageActionWithAuth.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
abstract class PageStartUp(authUserId: RequestHeader => Option[Long] = Unit => None) extends BasePageController {
  /**
   * This function services page action which can be accessed by anonymous user.
   * It pre-process request, invoke concrete action logic and handle all exceptions.
   *
   * @param f Concrete page action logic.
   * @return Action[AnyContent] Play action object.
   */
  def PageAction(f: Request[AnyContent] => Result): Action[AnyContent] = {
    Action {
      implicit request =>
        val start = System.currentTimeMillis
        var userIdPrefix = ""

        try {
          authUserId(request).foreach(id => userIdPrefix = s"[$id] ")
          Log.info(s"${userIdPrefix}Calling page action: ${request.method} ${request.uri}")
          f(request)
        } catch {
          case e: BaseException => e.handleResult(InternalServerError(views.html.mvc.error500()))
          case e: Throwable =>
            Log.error(s"${userIdPrefix}Unknown internal error happened.", e)
            InternalServerError(views.html.mvc.error500())
        } finally {
          Log.info(s"${userIdPrefix}Page action executed: ${request.uri}, TIME: ${System.currentTimeMillis - start}")
        }
    }
  }

  /**
   * This function services page action which only can be accessed by login user.
   * It pre-process request, invoke concrete action logic and handle all exceptions.
   *
   * @param f Concrete page action logic.
   * @return Action[AnyContent] Play action object.
   */
  def PageActionWithAuth(f: => Long => (Request[AnyContent]) => Result): Action[AnyContent] = {
    Action {
      implicit request =>
        val start = System.currentTimeMillis
        var userIdPrefix = ""

        try {
          authUserId(request) match {
            case Some(id) =>
              userIdPrefix = s"[$id] "
              Log.info(s"${userIdPrefix}Calling page action: ${request.method} ${request.uri}")
              f(id)(request)
            case _ =>
              Log.warn(s"Anonymous user try to access page: ${request.method} ${request.uri}")
              Forbidden(views.html.mvc.error404())
          }
        } catch {
          case e: BaseException => e.handleResult(InternalServerError(views.html.mvc.error500()))
          case e: Throwable =>
            Log.error(s"${userIdPrefix}Unknown internal error happened.", e)
            InternalServerError(views.html.mvc.error500())
        } finally {
          Log.info(s"${userIdPrefix}Page action executed: ${request.uri}, TIME: ${System.currentTimeMillis - start}")
        }
    }
  }
}