package user.api

import auth._
import core.mvc._
import core.mvc.Form._
import core.common._

/**
 * API for serving user authentication and authorization related requests.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object AuthAPI extends APIStartUp(auth) {
  val registerForm = Form(
    'email -> string(max = 100, reg = """([\w\.-]+)@([\w\.-]+)"""),
    'nickname -> string(min = 4, max = 20),
    'password -> string(min = 6, max = 20),
    'birthday -> date(),
    'gender -> int(min = 1, max = 2))

  /**
   * Handle user register.
   */
  def register = APIAction {
    implicit request =>
      val form = registerForm.bind()
      Log.info(">>>>>>>>>>>>>>>>>>> email: " + form('email))
      Log.info(">>>>>>>>>>>>>>>>>>> nickname: " + form('nickname))
      Log.info(">>>>>>>>>>>>>>>>>>> password: " + form('password))
      Log.info(">>>>>>>>>>>>>>>>>>> birthday: " + form('birthday))
      Log.info(">>>>>>>>>>>>>>>>>>> gender: " + form('gender))

      Ok()
  }
}
