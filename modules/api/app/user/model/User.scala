package user.model

import java.util.Date

import core.db._

/**
 * User model definition.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class User(var id: Option[Long] = None,
                var uuid: String,
                var status: Int,
                var createDate: Date = new Date(),
                var updateDate: Option[Date] = None,
                var del: Boolean = false,
                var note: String = "") extends Model

/**
 * User DAO definition.
 *
 * @author Clay Zhong
 * @version 1.0.0 
 */
object UserDao extends Dao[User](table = "user") {
  protected val parser = {
    column[Option[Long]]("id") ~
      column[String]("uuid") ~
      column[Int]("status") ~
      column[Date]("create_date", "createDate") ~
      column[Option[Date]]("update_date", "updateDate") ~
      column[Boolean]("del", "del") ~
      column[String]("note") map {
      case id ~ uuid ~ status ~ createDate ~ updateDate ~ del ~ note =>
        User(id, uuid, status, createDate, updateDate, del, note)
    }
  }

  def getByUUID(uuid: String) = getModel(SQL where "uuid = {uuid}")('uuid -> uuid)
}