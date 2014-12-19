package core.thread

/**
 * Operations which required synchronized support in whole system.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Operations extends Enumeration {
  /**
   * Login bonus, user can only get one each day.
   */
  val LoginBonus = Value("loginBonus")
}