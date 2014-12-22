package core.mvc


import scala.collection.mutable
import play.api.mvc.{AnyContent, Request}

import core.common.{Log, ParamException}
import core.utils.StringUtil
import core.utils.DateUtil

/**
 * Holds all form validator and mappings.
 * Check and get date from HTTP post request.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
case class FormData() {
  var fields = mutable.LinkedHashMap.empty[String, (String, Option[String]) => String]

  /**
   * Add field definition in form.
   *
   * @param field Field name.
   * @param validator Field validator.
   */
  def add(field: String, validator: (String, Option[String]) => String) = {
    this.fields += (field -> validator)
  }

  /**
   * Bind form with current object.
   * Validate all form fields and return data map base on form definition.
   *
   * @param multipart Whether form is multipart.
   * @param request HTTP request.
   * @return Parameter value map.
   */
  def bind(multipart: Boolean = false)(implicit request: Request[AnyContent]): Map[Symbol, String] = {
    val params = this.getPostParams(multipart)

    this.fields.map(field => {
      val (name, validator) = field
      Log.debug(s"Checking field: $name")

      val value = params.get(name) match {
        case Some(seq) => validator(name, Some(seq(0)))
        case _ => validator(name, None)
      }

      (Symbol(name), value)
    }).toMap[Symbol, String]
  }

  /**
   * Get all post data from HTTP request body.
   *
   * @param multipart Whether form is multipart.
   * @param request HTTP request.
   * @return Parameter value map in request.
   */
  def getPostParams(multipart: Boolean)(implicit request: Request[AnyContent]): Map[String, Seq[String]] = multipart match {
    case true => request.body.asMultipartFormData.get.asFormUrlEncoded
    case false => request.body.asFormUrlEncoded match {
      case Some(data) => data
      case _ => throw new ParamException("No data found in post request body.")
    }
  }
}

/**
 * HTTP post form data validation.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Form {
  /**
   * Generate Form data and validator according to form definition.
   *
   * @param params Field type definition.
   * @return Generated form data class.
   */
  def apply(params: (Symbol, (String, Option[String]) => String)*) = {
    val formData = FormData()

    params.foreach(param => {
      formData.add(param._1.name, param._2)
    })

    formData
  }

  /**
   * String value validator.
   *
   * @param required Whether field is not null.
   * @param min Minimum character length.
   * @param max Maximum character length.
   * @param reg Regex for string validation.
   * @param default Default value if field is not giving.
   * @return String validator function.
   */
  def string(required: Boolean = true, min: Int = Int.MinValue, max: Int = Int.MaxValue, reg: String = "",
             default: Option[String] = None): (String, Option[String]) => String = (field, valueOpt) => {
    var value = valueOpt.getOrElse("")

    if (StringUtil.isBlank(value)) {
      if (default.isDefined) value = default.get
      else if (required) throw new ParamException(s"$field is required.")
    }

    if ((value.length < min) || (value.length > max))
      throw new ParamException(s"$field length must between $min and $max.")

    if (StringUtil.isNotBlank(reg) && !value.matches(reg))
      throw new ParamException(s"$field does not match required format.")

    value
  }

  /**
   * Integer value validator.
   *
   * @param required Whether field is not null.
   * @param min Minimum value.
   * @param max Maximum value.
   * @param default Default value if field is not giving.
   * @return Integer validator function.
   */
  def int(required: Boolean = true, min: Int = Int.MinValue, max: Int = Int.MaxValue,
          default: Option[Int] = None): (String, Option[String]) => String = (field, valueOpt) => {
    var value = valueOpt.getOrElse("")

    if (StringUtil.isBlank(value)) {
      if (default.isDefined) value = default.get.toString
      else if (required) throw new ParamException(s"$field is required.")
    }

    if (value.exists(!_.isDigit))
      throw new ParamException(s"$field is not a number value.")

    if (value.toInt < min || value.toInt > max)
      throw new ParamException(s"$field value must between $min and $max.")

    value
  }

  /**
   * Boolean value validator.
   *
   * @param required Whether field is not null.
   * @param default Default value if field is not giving.
   * @return Boolean validator function.
   */
  def boolean(required: Boolean = true, default: Option[Int] = None): (String, Option[String]) => String = (field, valueOpt) => {
    var value = valueOpt.getOrElse("")

    if (StringUtil.isBlank(value)) {
      if (default.isDefined) value = default.get.toString
      else if (required) throw new ParamException(s"$field is required.")
    }

    value match {
      case "true" | "1" => true.toString
      case "false" | "0" => false.toString
      case _ => throw new ParamException(s"$field is not a boolean value.")
    }
  }

  /**
   * Date or datetime value validator.
   *
   * @param required Whether field is not null.
   * @param format Expect date format.
   * @param default Default value if field is not giving.
   * @return Date validator function.
   */
  def date(required: Boolean = true, format: String = DateUtil.Format.YYYY_MM_DD,
           default: Option[String] = None): (String, Option[String]) => String = (field, valueOpt) => {
    var value = valueOpt.getOrElse("")

    if (StringUtil.isBlank(value)) {
      if (default.isDefined) value = default.get
      else if (required) throw new ParamException(s"$field is required.")
    }

    try {
      DateUtil.str2Date(value, format)
    } catch {
      case e: Exception => throw new ParamException(s"$field does not match date format $format.")
    }

    value
  }
}
