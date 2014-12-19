package user.api

import auth._
import core.mvc.APIStartUp
import user.common.UserService
import user.model.UserDao

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
  def getMyProfile = APIActionWithAuth {
    userId => implicit request =>
      Ok(this.getProfile(userId))
  }

  /**
   * Get others profile info.
   */
  def getUserProfile(userId: Long) = APIAction {
    implicit request =>
      Ok(this.getProfile(userId))
  }

  private def getProfile(userId: Long): Map[Symbol, Any] = {
    val user = UserDao.load(userId).get
    val profile = UserService.getProfile(userId)

    Map(
      'nickname -> profile.nickname,
      'mail -> profile.mail,
      'birthday -> profile.birthday,
      'gender -> profile.gender,
      'status -> user
    )
  }
}
