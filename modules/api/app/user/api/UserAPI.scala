package user.api

import auth._
import core.mvc.APIStartUp
import core.utils.StringUtil
import user.common._
import user.model._

/**
 * API for serving user module related requests.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object UserAPI extends APIStartUp(auth) {
  /**
   * Get current login user's profile info. 
   */
  def getMyProfile = APIActionWithAuth { userId => implicit request =>
    Ok(this.getProfile(userId))
  }

  /**
   * Get others profile info.
   */
  def getUserProfile(userId: Long) = APIAction { implicit request =>
    Ok(this.getProfile(userId))
  }

  private def getProfile(userId: Long): Map[Symbol, Any] = {
    val user = UserDao.load(userId).get
    val profile = UserService.getProfile(userId)

    Map(
      'nickname -> profile.nickname,
      'mail -> user.mail,
      'birthday -> profile.birthday,
      'gender -> profile.gender,
      'status -> user)
  }

  /**
   * Demo API for model retrieving, creating and deleting.
   * Also contains the manual for how to response data and validate parameters. 
   */

  def simple = APIAction { implicit request =>
    Ok()
  }

  def withStatusCode = APIAction { implicit request =>
    Ok(UserStatusCode.ACCESS_DENY)
  }

  def withAuth = APIActionWithAuth { userId => implicit request =>
    Ok()
  }

  def withData = APIAction { implicit request =>
    Ok(Map(
      'name -> "jalay",
      'version -> "1.0.0"))
  }

  def withComplexData = APIAction { implicit request =>
    val result = LinkedHashMap(
      'name -> "jalay",
      'version -> "1.0.0",
      'developers -> List(
        Map(
          'name -> "Clay",
          'age -> 30)))

    Ok(UserStatusCode.SUCCESS, result)
  }

  def getUser(id: Int) = APIAction { implicit request =>
    UserDao.load(id) match {
      case Some(user) => Ok(LinkedHashMap(
        'id -> user.id,
        'uuid -> user.uuid,
        'mail -> user.mail
      ))
      case _ => Ok(UserStatusCode.DATA_NOT_FOUND)
    }
  }

//  def create = APIAction { implicit request =>
//    val id = UserDao.save(User(
//      uuid = uuid,
//      mail = mail,
//      password = StringUtil.md5(form('password)),
//      status = UserConstant.UserStatus.ACTIVE))
//
//
//
//
//    val user = UserDao.save(
//      User(
//        string = "string",
//        int = 1,
//        long = 1,
//        bool = true))
//
//    val result = Map('id -> newId)
//
//    Ok(result)
//  }
}
