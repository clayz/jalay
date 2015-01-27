package user.common

import core.mvc.StatusCode

/**
 * API status code definition for user module.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object UserStatusCode extends StatusCode {
  // login errors
  val LOGIN_FAILED = ("USR000001", "Mail does not exist or password incorrect.")
  val LOGIN_MAIL_NOT_EXISTS = ("USR000002", "Mail address does not exist.")
  val LOGIN_PASSWORD_INCORRECT = ("USR000003", "Incorrect login password.")

  // register errors
  val REG_MAIL_EXISTS = ("USR000011", "Register mail address already exists.")
  val REG_NICKNAME_EXISTS = ("USR000012", "Register nickname already exists.")
}
