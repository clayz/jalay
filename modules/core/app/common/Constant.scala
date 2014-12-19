package core.common

import java.util.Date
import scala.concurrent.duration.Duration
import core.utils.DateUtil

/**
 * System common constants.
 *
 * This class is used to define all constants for core system module.
 * Other constants which be used for certain subsystem, please create corresponding constant file
 * in subsystems' common package.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Constant {

  /**
   * Http constants.
   */
  object Http {
    val TIMEOUT_DURATION = Duration(1000 * 10, "millis")

    object ContentType extends Enumeration {
      val JSON = Value("application/json; charset=utf-8")
      val XML = Value("application/xml; charset=utf-8")
      val TEXT = Value("text/plain; charset=utf-8")
      val JS = Value("application/x-javascript; charset=utf-8")
      val UNDEFINED = Value("")
    }

  }

  /**
   * Database constants.
   */
  object DB {
    val DEFAULT_ID = 0
    val DEFAULT_DATE = "0000-00-00"
    val DEFAULT_DATETIME = "0000-00-00 00:00:00"

    // used to get all data from database
    val LIMIT_ALL = -1

    // default offset and limit for list data
    val DEFAULT_QUERY_OFFSET = 0
    val DEFAULT_QUERY_LIMIT = 10

    // default number of transaction commit
    val DEFAULT_COMMIT_NUMBER = 5000
  }

}