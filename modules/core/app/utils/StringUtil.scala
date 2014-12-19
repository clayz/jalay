package core.utils

import scala.util.Random
import org.apache.commons.lang.StringUtils
import core.common.Log

/**
 * String operation utilities.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object StringUtil {
  // all available chars for generating random string
  val CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

  /**
   * Get Random string value.
   *
   * @param length Length of target random string
   * @param chars Available chars.
   * @return String Random string value.
   */
  def random(length: Int, chars: String = CHARS): String =
    (for (i <- 1 to length) yield chars.charAt(Random.nextInt(chars.size))).mkString

  /**
   * Get random unique string value.
   *
   * @param length Length of target random string
   * @param chars Available chars.
   * @param block Block of code for checking whether string is unique.
   * @return String Random string value.
   */
  def randomUnique(length: Int, chars: String = CHARS)(block: String => Boolean): String = {
    var value = (for (i <- 1 to length) yield chars.charAt(Random.nextInt(chars.size))).mkString
    while (!block(value)) value = (for (i <- 1 to length) yield chars.charAt(Random.nextInt(chars.size))).mkString
    value
  }

  /**
   * Generate UUID string.
   *
   * @return String Generated UUID value.
   */
  def uuid: String = java.util.UUID.randomUUID.toString.replaceAll("-", "")

  def isEmpty(str: String): Boolean = StringUtils.isEmpty(str)

  def isNotEmpty(str: String): Boolean = StringUtils.isNotEmpty(str)

  def isBlank(str: String): Boolean = StringUtils.isBlank(str)

  def isNotBlank(str: String): Boolean = StringUtils.isNotBlank(str)


}