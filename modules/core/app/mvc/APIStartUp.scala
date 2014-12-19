package core.mvc

import core.common._
import play.api.mvc._

import scala.collection.mutable.LinkedHashMap

/**
 * All API actions in subsystems should extends from this class.
 * It provides basic action functions, user authentication and parameters validation.
 *
 * Implement the userId function attribute in constructor if you want to use APIActionWithAuth.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
abstract class APIStartUp(authUserId: RequestHeader => Option[Long] = Unit => None) extends BaseAPIController {
  /**
   * This function services API action which can be accessed by anonymous user.
   * It pre-process request, invoke concrete action logic and handle all exceptions.
   *
   * @param f Concrete API action logic.
   * @return Action[AnyContent] Play action object.
   */
  def APIAction(f: Request[AnyContent] => Result): Action[AnyContent] = {
    Action {
      implicit request =>
        val start = System.currentTimeMillis
        var userIdPrefix = ""

        try {
          authUserId(request).foreach(id => userIdPrefix = s"[$id] ")
          Log.info(s"${userIdPrefix}Calling API action: ${request.method} ${request.uri}")
          f(request)
        } catch {
          case e: APIException => e.handleResult(Ok(e.statusCode))
          case e: ParamException => e.handleResult(Ok(CommonStatusCode.PARAMETER_ERROR, e.message))
          case e: BaseException => e.handleResult(Ok(CommonStatusCode.ERROR))
          case e: Exception =>
            Log.error(s"${userIdPrefix}Unknown internal error happened.", e)
            Ok(CommonStatusCode.ERROR)
        } finally {
          Log.info(s"${userIdPrefix}API action executed: ${request.uri}, TIME: ${System.currentTimeMillis - start}")
        }
    }
  }

  /**
   * This function services API action which only can be accessed by login user.
   * It pre-process request, invoke concrete action logic and handle all exceptions.
   *
   * @param f Concrete API action logic.
   * @return Action[AnyContent] Play action object.
   */
  def APIActionWithAuth(f: => Long => (Request[AnyContent]) => Result): Action[AnyContent] = {
    Action {
      implicit request =>
        val start = System.currentTimeMillis
        var userId: Option[Long] = None
        var userIdPrefix = ""

        try {
          userId = authUserId(request)
          userId match {
            case Some(id) =>
              userIdPrefix = s"[$id] "
              Log.info(s"${userIdPrefix}Calling API action: ${request.method} ${request.uri}")
              f(id)(request)
            case _ =>
              Log.warn(s"Anonymous user try to access API: ${request.method} ${request.uri}")
              Ok(CommonStatusCode.ACCESS_DENY)
          }
        } catch {
          case e: APIException => e.handleResult(Ok(e.statusCode), userId)
          case e: ParamException => e.handleResult(Ok(CommonStatusCode.PARAMETER_ERROR, e.message), userId)
          case e: BaseException => e.handleResult(Ok(CommonStatusCode.ERROR), userId)
          case e: Exception =>
            Log.error(s"${userIdPrefix}Unknown internal error happened.", e)
            Ok(CommonStatusCode.ERROR)
        } finally {
          Log.info(s"${userIdPrefix}API action executed: ${request.uri}, TIME: ${System.currentTimeMillis - start}")
        }
    }
  }
}
