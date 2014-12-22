package user.common

import core.mvc.StatusCode

/**
 * API status code definition for user module.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object UserStatusCode extends StatusCode {
  // login and register
  lazy val REG_MAIL_EXISTS = ("USR000001", "Register mail address already exists.")
  lazy val REG_NICKNAME_EXISTS = ("USR000002", "Register nickname already exists.")
}
