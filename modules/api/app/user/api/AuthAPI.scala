package user.api

import core.db._
import core.mvc._
import core.mvc.Form._
import core.common.APIException
import core.utils._
import auth._
import user.common._
import user.model._

/**
 * API for serving user authentication and authorization related requests.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object AuthAPI extends APIStartUp(auth) {
  val loginForm = Form(
    'mail -> string(max = 100, reg = """([\w\.-]+)@([\w\.-]+)"""),
    'password -> string(min = 6, max = 20),
    'remember -> boolean(default = Some(false)))

  /**
   * Handle user login.
   */
  def login = APIAction { implicit request =>
    val form = loginForm.bind()

    UserDao.getByMail(form('mail)) match {
      case Some(user) if user.password == StringUtil.md5(form('password)) => Ok()
      case _ => throw new APIException(UserStatusCode.LOGIN_FAILED)
    }
  }

  val registerForm = Form(
    'mail -> string(max = 100, reg = """([\w\.-]+)@([\w\.-]+)"""),
    'nickname -> string(min = 4, max = 20),
    'password -> string(min = 6, max = 20),
    'birthday -> date(),
    'gender -> int(min = 1, max = 2))

  /**
   * Handle user register.
   */
  def register = APIAction { implicit request =>
    val form = registerForm.bind()
    val mail = form('mail)
    val nickname = form('nickname)

    if (UserDao.getByMail(mail).isDefined)
      throw new APIException(UserStatusCode.REG_MAIL_EXISTS)
    if (UserProfileDao.getByNickname(nickname).isDefined)
      throw new APIException(UserStatusCode.REG_NICKNAME_EXISTS)

    val uuid = StringUtil.uuid

    DB.withTransaction() { implicit connection =>
      val id = UserDao.save(User(
        uuid = uuid,
        mail = mail,
        password = StringUtil.md5(form('password)),
        status = UserConstant.UserStatus.ACTIVE))

      UserProfileDao.save(UserProfile(
        userId = id,
        nickname = nickname,
        birthday = DateUtil.str2Date(form('birthday)),
        gender = form('gender).toInt))
    }

    Ok()
  }
}
