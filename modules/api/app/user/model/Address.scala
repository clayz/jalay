package user.model

import java.util.Date
import core.db._

/**
 * Address model definition.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class Address(var id: Option[Long] = None,
                   var userId: Long,
                   var addressType: Int,
                   var zipCode: String,
                   var street: String,
                   var createDate: Date = new Date(),
                   var updateDate: Option[Date] = None,
                   var del: Boolean = false,
                   var note: String = "") extends Model

/**
 * Address DAO definition.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object AddressDao extends Dao[Address](table = "address") {
  protected val parser = {
    column[Option[Long]]("id") ~
      column[Long]("user_id", "userId") ~
      column[Int]("address_type", "addressType") ~
      column[String]("zip_code", "zipCode") ~
      column[String]("street", "street") ~
      column[Date]("create_date", "createDate") ~
      column[Option[Date]]("update_date", "updateDate") ~
      column[Boolean]("del", "del") ~
      column[String]("note") map {
      case id ~ userId ~ addressType ~ zipCode ~ street ~ createDate ~ updateDate ~ del ~ note =>
        Address(id, userId, addressType, zipCode, street, createDate, updateDate, del, note)
    }
  }
}