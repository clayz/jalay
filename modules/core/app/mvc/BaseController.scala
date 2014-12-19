package core.mvc

import scala.collection._
import play.api.libs.json._
import play.api.mvc._

import core.common.Constant.Http._
import core.common._
import core.utils.{StringUtil, Jsonic}

/**
 * Base Controller for serving API action requests.
 * It receives API response data, add status code and message, then parse the data to JSON or XML format for client.
 * This class override Play's common result functions such as Ok.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
trait BaseAPIController extends Controller {
  /**
   * Response empty data with success status.
   *
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok()(implicit request: RequestHeader): Result = {
    this.response(OK)
  }

  /**
   * Response empty data with success status.
   *
   * @param contentType Content type of response data.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok(contentType: ContentType.Value)(implicit request: RequestHeader): Result = {
    this.response(OK, contentType = contentType)
  }

  /**
   * Response with certain status code.
   *
   * @param statusCode Customized status code.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok(statusCode: (String, String))(implicit request: RequestHeader): Result = {
    this.response(OK, statusCode)
  }

  /**
   * Response with certain status code and customized message.
   * This message will replace the default message in status code.
   *
   * @param statusCode Customized status code.
   * @param message Customized message.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok(statusCode: (String, String), message: String)(implicit request: RequestHeader): Result = {
    this.response(OK, statusCode, Some(message))
  }

  /**
   * Response with data map.
   *
   * @param responseData Response data map.
   * @param contentType Content type of response data.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok(responseData: Map[Symbol, Any], contentType: ContentType.Value)(implicit request: RequestHeader): Result = {
    this.response(OK, responseData = Some(responseData), contentType = contentType)
  }

  /**
   * Response with data map.
   *
   * @param responseData Response data map.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok(responseData: Map[Symbol, Any])(implicit request: RequestHeader): Result = {
    this.response(OK, responseData = Some(responseData))
  }

  /**
   * Response with data map and customized message.
   *
   * @param responseData Response data map.
   * @param message Customized message.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok(responseData: Map[Symbol, Any], message: String)(implicit request: RequestHeader): Result = {
    this.response(OK, message = Some(message), responseData = Some(responseData))
  }

  /**
   * Response with customized status code and data map.
   *
   * @param statusCode Customized status code.
   * @param responseData Response data map.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok(statusCode: (String, String), responseData: Map[Symbol, Any])(implicit request: RequestHeader): Result = {
    this.response(OK, statusCode, responseData = Some(responseData))
  }

  /**
   * Response with customized status code and message, and data map.
   *
   * @param statusCode Customized status code.
   * @param responseData Response data map.
   * @param message Customized message.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Ok(statusCode: (String, String), responseData: Map[Symbol, Any], message: String)(implicit request: RequestHeader): Result = {
    this.response(OK, statusCode, message = Some(message), responseData = Some(responseData))
  }

  /**
   * Response with plain data map.
   *
   * @param responseData Response data map.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def PlainOk(responseData: Map[Symbol, Any])(implicit request: RequestHeader): Result = {
    this.response(OK, responseData = Some(responseData), isPlain = true)
  }

  /**
   * Response with JSONP data.
   *
   * @param callback JSONP callback function.
   * @param responseData Response data map.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def JSONP(callback: String, responseData: Map[Symbol, Any])(implicit request: RequestHeader): Result = {
    val data = this.mapToJson(CommonStatusCode.SUCCESS, data = Some(responseData), isPlain = true)

    Ok(callback match {
      case "?" => s"($data)"
      case function => s"$function($data)"
    }).as(ContentType.JS.toString)
  }

  /**
   * Response 500 error with default error code.
   *
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Error()(implicit request: RequestHeader): Result = {
    this.response(INTERNAL_SERVER_ERROR, CommonStatusCode.ERROR)
  }

  /**
   * Response 500 error with customized status code and message.
   *
   * @param statusCode Customized status code.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Error(statusCode: (String, String))(implicit request: RequestHeader): Result = {
    this.response(INTERNAL_SERVER_ERROR, statusCode)
  }

  /**
   * Response 500 error with customized message.
   *
   * @param message Customized message.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Error(message: String)(implicit request: RequestHeader): Result = {
    this.response(INTERNAL_SERVER_ERROR, CommonStatusCode.ERROR, Some(message))
  }

  /**
   * Response 500 error with customized status code and message.
   *
   * @param statusCode Customized status code.
   * @param message Customized message.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  def Error(statusCode: (String, String), message: String)(implicit request: RequestHeader): Result = {
    this.response(INTERNAL_SERVER_ERROR, statusCode, Some(message))
  }

  /**
   * Get parameter from HTTP GET request by name.
   *
   * @param name Target parameter name.
   * @param request Http request.
   * @return String Parameter value.
   */
  def getParameter(name: String)(implicit request: Request[AnyContent]): String = request.queryString(name)(0)

  /**
   * Get all parameters from HTTP GET request.
   *
   * @param request Http request.
   * @return Map[String, Seq[String] All parameter values.
   */
  def getParameters(implicit request: Request[AnyContent]): Map[String, Seq[String]] = request.queryString

  /**
   * Get parameter from HTTP POST request by name.
   *
   * @param name Target parameter name.
   * @param request Http request.
   * @return String Parameter value.
   */
  def getPostParameter(name: String)(implicit request: Request[AnyContent]): String = request.body.asFormUrlEncoded.get(name)(0)

  /**
   * Get all parameters from HTTP POST request.
   *
   * @param request Http request.
   * @return Map[String, Seq[String] All parameter values.
   */
  def getPostParameters(implicit request: Request[AnyContent]): Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map())

  /**
   * Generate actual response data as JSON or XML.
   * It will find attribute "Response-Format" in HTTP header to determine response format type.
   *
   * @param httpStatus HTTP status code.
   * @param statusCode Customized API status code.
   * @param message Customized message.
   * @param responseData Response data map.
   * @param isPlain Only response data in data map, no need extra data such as status code, message.
   * @param request Current HTTP request.
   * @return Result Play HTTP response result.
   */
  private def response(httpStatus: Int, statusCode: (String, String) = CommonStatusCode.SUCCESS, message: Option[String] = None,
                       responseData: Option[Map[Symbol, Any]] = None, isPlain: Boolean = false, contentType: ContentType.Value = ContentType.UNDEFINED)(
                        implicit request: RequestHeader): Result = contentType match {
    case ContentType.UNDEFINED => request.headers.get("Response-Format") match {
      case None | Some("json") => new Status(httpStatus)(mapToJson(statusCode, message, responseData, isPlain)).as(ContentType.JSON.toString)
      case Some("xml") => new Status(httpStatus)(mapToXML(statusCode, message, responseData, isPlain)).as(ContentType.XML.toString)
      case Some("text") => new Status(httpStatus)(mapToJson(statusCode, message, responseData, isPlain)).as(ContentType.TEXT.toString)
      case _ => throw new APIException(CommonStatusCode.UNSUPPORTED_RESPONSE_FORMAT)
    }
    case ContentType.JSON => new Status(httpStatus)(mapToJson(statusCode, message, responseData, isPlain)).as(ContentType.JSON.toString)
    case ContentType.XML => new Status(httpStatus)(mapToXML(statusCode, message, responseData, isPlain)).as(ContentType.XML.toString)
    case ContentType.TEXT => new Status(httpStatus)(mapToJson(statusCode, message, responseData, isPlain)).as(ContentType.TEXT.toString)
    case _ => throw new APIException(CommonStatusCode.UNSUPPORTED_RESPONSE_FORMAT)
  }


  /**
   * Parse status code, message and data map to JSON string.
   *
   * @param statusCode API status code.
   * @param message Customized message.
   * @param data API response data map.
   * @param isPlain Only response data in data map, no need extra data such as status code, message.
   * @return String JSON value after parsing.
   */
  private def mapToJson(statusCode: (String, String), message: Option[String] = None, data: Option[Map[Symbol, Any]], isPlain: Boolean): String = {
    val responseMap = mutable.LinkedHashMap.empty[String, JsValue]

    if (!isPlain) {
      // add status code and message
      responseMap += ("status" -> Jsonic.jsonValue(statusCode._1))
      responseMap += ("message" -> Jsonic.jsonValue({
        val showMessage = new StringBuilder

        if (StringUtil.isNotBlank(statusCode._2))
          showMessage.append(statusCode._2)

        if (message.isDefined) {
          if (!showMessage.isEmpty) showMessage.append(" ")
          showMessage.append(message.get)
        }

        showMessage.toString
      }))
    }

    // parse data map to JSON
    data match {
      case Some(dataMap) =>
        if (isPlain) {
          dataMap.foreach(entry => responseMap += (entry._1.name -> Jsonic.jsonValue(entry._2)))
        } else {
          val seq = mutable.ListBuffer.empty[(String, JsValue)]
          dataMap.foreach(entry => seq += (entry._1.name -> Jsonic.jsonValue(entry._2)))
          responseMap += ("result" -> JsObject(seq.toSeq))
        }
      case _ => responseMap
    }

    Jsonic.toJson(responseMap)
  }

  /**
   * Parse status code, message and data map to XML string.
   *
   * @param statusCode API status code.
   * @param message Customized message.
   * @param data API response data map.
   * @return String XML value after parsing.
   */
  private def mapToXML(statusCode: (String, String), message: Option[String] = None, data: Option[Map[Symbol, Any]], isPlain: Boolean): String = {
    throw new ParamException("XML response format is not supported now.")
  }
}

/**
 * Base Controller for serving page action requests.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
trait BasePageController extends Controller {

}