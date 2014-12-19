package core

/**
 * Contains DB common classes and functions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
package object db {

  import core.db.{DBConnection => dc}

  /**
   * Anorm funtion delegator.
   */
  val ~ = anorm ~

  /**
   * Renamed DBConnection for conveniences using.
   */
  val DB = dc
}