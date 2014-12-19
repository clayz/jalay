package user.common

import play.api.mvc.RequestHeader
import core.common._
import user.model._
import user.common.UserConstant.UserStatus

/**
 * Common services for user and authentication module.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object UserService {
  def getLoginUserId(implicit request: RequestHeader): Option[Long] = {
    Log.debug(s"Request cookies: ${request.cookies.toString()}")

    val uuid = request.cookies.get("uuid")
    if (uuid.isEmpty) return None

    UserDao.getByUUID(uuid.get.value) match {
      case Some(user) =>
        user.status match {
          case UserStatus.INACTIVE | UserStatus.WITHDRAWAL =>
            Log.warn(s"User [${user.id.get}] is withdrawal or invalid.")
            None
          case _ => user.id
        }
      case _ => None
    }
  }

  def getUser(userId: Long): User =
    UserDao.load(userId).getOrElse(throw new DataException(s"User [$userId] not found."))

  def getProfile(userId: Long): UserProfile =
    UserProfileDao.getByUser(userId).getOrElse(throw new DataException(s"User [$userId] profile not found."))
}
