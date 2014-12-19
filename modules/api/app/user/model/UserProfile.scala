package user.model

import java.util.Date
import core.db._
import user.common.UserConstant.Gender

/**
 * User profile model definition.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class UserProfile(var id: Option[Long] = None,
                       var userId: Long,
                       var nickname: String,
                       var mail: String,
                       var birthday: Option[Date] = None,
                       var gender: Int = Gender.FEMALE,
                       var createDate: Date = new Date(),
                       var updateDate: Option[Date] = None,
                       var del: Boolean = false,
                       var note: String = "") extends Model

/**
 * User profile DAO definition.
 *
 * @author Clay Zhong
 * @version 1.0.0 
 */
object UserProfileDao extends Dao[UserProfile](table = "user_profile") {
  protected val parser = {
    column[Option[Long]]("id") ~
      column[Long]("user_id", "userId") ~
      column[String]("nickname") ~
      column[String]("mail") ~
      column[Option[Date]]("birthday", defaultValue = "0000-00-00") ~
      column[Int]("gender") ~
      column[Date]("create_date", "createDate") ~
      column[Option[Date]]("update_date", "updateDate") ~
      column[Boolean]("del", "del") ~
      column[String]("note") map {
      case id ~ userId ~ nickname ~ mail ~ birthday ~ gender ~ createDate ~ updateDate ~ del ~ note =>
        UserProfile(id, userId, nickname, mail, birthday, gender, createDate, updateDate, del, note)
    }
  }

  def getByUser(userId: Long) = getModel(SQL where "user_id = {userId}")('userId -> userId)
}