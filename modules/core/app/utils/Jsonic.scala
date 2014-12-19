package core.utils

import java.util.Date
import play.api.libs.json._
import collection.mutable
import collection.mutable.LinkedHashMap
import core.common.CommonException
import core.common.Log
import java.math.BigInteger

/**
 * Provides utilities functions for handling JSON data.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Jsonic {
  /**
   * Convert a LinkedHashMap[String,JsValue] to a JSON string.
   *
   * @param map Map need to convert to a JSON string
   * @return a JSON string
   */
  def toJson(map: LinkedHashMap[String, JsValue]): String = {
    val jsonObj = Json.toJson(map.toMap)
    Json.stringify(jsonObj)
  }

  /**
   * Convert any Scala data to JsValue.
   *
   * @param value Any Scala object.
   * @return JsValue Play JSON value.
   */
  def jsonValue(value: Any): JsValue = {
    try {
      value match {
        case _: String => JsString(value.asInstanceOf[String])
        case _: Int => JsNumber(value.asInstanceOf[Int])
        case _: Long => JsNumber(value.asInstanceOf[Long])
        case _: Float => JsNumber(value.asInstanceOf[Float])
        case _: Double => JsNumber(value.asInstanceOf[Double])
        case _: Boolean => JsBoolean(value.asInstanceOf[Boolean])
        case _: Date => JsString(DateUtil.date2Str(value.asInstanceOf[Date]))
        case _: BigInteger => JsNumber(value.asInstanceOf[BigInteger].longValue)
        case _: Option[_] => jsonValue(value.asInstanceOf[Option[_]].orNull)
        case _: Map[_, _] =>
          val seq = mutable.ListBuffer.empty[(String, JsValue)]
          value.asInstanceOf[Map[Symbol, _]].foreach(entry => seq += (entry._1.name -> jsonValue(entry._2)))
          JsObject(seq.toSeq)
        case _: LinkedHashMap[_, _] =>
          val seq = mutable.ListBuffer.empty[(String, JsValue)]
          value.asInstanceOf[LinkedHashMap[Symbol, _]].foreach(entry => seq += (entry._1.name -> jsonValue(entry._2)))
          JsObject(seq.toSeq)
        case _: List[_] =>
          val seq = mutable.ListBuffer.empty[JsValue]
          value.asInstanceOf[List[_]].foreach(data => seq += jsonValue(data))
          JsArray(seq)
        case null => JsNull
        case _ =>
          Log.warn("Unsupported data type: " + value.toString)
          JsNull
      }
    } catch {
      case e: Throwable =>
        Log.error("Parse JSON value failed: " + value.toString)
        throw new CommonException(e)
    }
  }
}