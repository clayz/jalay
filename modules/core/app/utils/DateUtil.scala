package core.utils

import java.text.SimpleDateFormat
import java.util._
import core.common.CommonException

/**
 * Date handling and formatting utility functions.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object DateUtil {

  /**
   * All supported data formatter.
   */
  object Format {
    val YYYY = "yyyy"
    val YYYYMM = "yyyyMM"
    val YYYYMMDD = "yyyyMMdd"
    val YYYY_MM_DD = "yyyy-MM-dd"
    val YYYY_MM_DD_P = "yyyy.MM.dd"
    val YYYY_MM_DD_ARTICLE = "yyyy_MM_dd"
    val MM_DD_YY = "MM/dd/yyyy"
    val DD = "dd"
    val MM_DD = "MM/dd"

    val HH_MI = "HH:mm"
    val HH_MI_SS = "HH:mm:ss"

    val YYYYMMDDHHMISS = "yyyyMMddHHmmss"
    val YYYY_MM_DD_HH_MI_SS = "yyyy-MM-dd HH:mm:ss"
    val YYYY_MM_DD_HH_MI = "yyyy-MM-dd HH:mm"
    val YYYY_MM_DD_HH = "yyyy-MM-dd HH"
    val MM_DD_YYYY_HH_MI_SS = "MM/dd/yyyy HH:mm:ss"

    val ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    val WDMYHMSZ = "EEE, d MMM yyyy HH:mm:ss Z"
    val WDMYHMSZ_DD = "EEE, dd MMM yyyy HH:mm:ss Z"
  }

  /**
   * Get current date with specified format.
   *
   * @return Current date after format.
   */
  def getNow(format: String) = new SimpleDateFormat(format) format new Date

  /**
   * Get current date time
   *
   * @return current date time
   */
  def getNow = Calendar.getInstance.getTime

  /**
   * Get date without hour and minutes.
   *
   * @param date Target date.
   * @return Date without hour and minutes.
   */
  def getDate(date: Date): Date = this.str2Date(this.date2Str(date, Format.YYYY_MM_DD))

  /**
   * Convert a string to date by specified format.
   *
   * @param date String Date string value.
   * @param format String Specified date format.
   * @return Date Date value after format.
   */
  def str2Date(date: String, format: String = Format.YYYY_MM_DD, local: Locale = Locale.JAPAN): Date = date match {
    case ds if (StringUtil.isNotBlank(date)) => try {
      new SimpleDateFormat(format, local).parse(date)
    } catch {
      case e: java.text.ParseException => new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(date)
    }
    case _ => throw new CommonException("Date value cannot be empty.")
  }

  /**
   * Convert a date to string by specified format.
   *
   * @param date Date Date value for format.
   * @param format String Specified date format.
   * @return String String value after format.
   */
  def date2Str(date: Date, format: String = Format.YYYY_MM_DD_HH_MI_SS, local: Locale = Locale.JAPAN): String = {
    if (date != null)
      if (StringUtil.isNotBlank(format)) new SimpleDateFormat(format, local).format(date)
      else new SimpleDateFormat(Format.YYYY_MM_DD_HH_MI_SS, local).format(date)
    else throw new CommonException("Date value cannot be null.")
  }

  /**
   * customer date ,add or minus days or months and return a new date time
   *
   * @param date ,base date
   * @param amount ,add or minus numbers
   * @param field ,add or minus year or month or day
   * @return a new date time
   */
  def add(date: Date, amount: Int, field: Int) = {
    val calendar = Calendar.getInstance
    calendar.setTime(date)
    calendar.add(field, amount)
    calendar.getTime
  }

  /**
   * Get the current date after several days of date.
   * If you need to get prior date, parameters with negative.
   * For example, get the date of last week on the same day, parameter is - 7
   *
   * @param days Add or minus days.
   * @return a new date.
   */
  def addDays(days: Int): Date = {
    add(new Date, days, Calendar.DATE)
  }

  /**
   * Get some days after the specified date time.
   * If you need to get prior date, parameters with negative.
   *
   * @param date Base date time.
   * @param days Add or minus days.
   * @return a new date.
   */
  def addDays(date: Date, days: Int): Date = {
    add(date, days, Calendar.DATE)
  }

  /**
   * Get some days after the specified date time.
   * If you need to get prior date, parameters with negative.
   *
   * @param date Base date time.
   * @param hours Add or minus hours.
   * @return a new date.
   */
  def addHours(date: Date, hours: Int) = {
    add(date, hours, Calendar.HOUR)
  }
}
