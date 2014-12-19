package core.sns

import java.io.File
import java.util.Date
import java.net.URLEncoder
import core.utils.HttpUtil

import scala.concurrent.Await
import play.api.libs.ws.WS
import play.api.libs.json.Json
import core.common._
import core.common.Constant.Http.TIMEOUT_DURATION

/**
 * Facebook API utility.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Facebook {
  val FOREIGN_TYPE = 2

  /**
   * Facebook configuration in application.conf
   */
  val clientId = AppConfig.get("ca.fb.client_id")
  val clientSecret = AppConfig.get("ca.fb.client_secret")

  /**
   * Facebook API urls.
   */
  object URL {
    val ACCESS_TOKEN = "https://graph.facebook.com/oauth/access_token"
    val ME = "https://graph.facebook.com/me?access_token=%s"
    val FRIENDS = "https://graph.facebook.com/me/friends?limit=10000000&access_token=%s"
    val PICTURE = "https://graph.facebook.com/me?access_token=%s&fields=picture"
    val FEED = "https://graph.facebook.com/me/feed?access_token=%s&message=%s"
    val POST_PHOTO = "https://graph.facebook.com/v2.1/me/photos?access_token=%s"
  }

  /**
   * Get Facebook user info.
   * Response map entries: id, nickname, birthday, mail
   *
   * @param token Facebook access token.
   * @param default Default user info if connect Facebook failed.
   * @param refresh Block of code for refresh and save token if current is expired.
   * @return User info map.
   */
  def me(token: String, default: Map[Symbol, String] = Map(), refresh: (Int, String) => String = (Int, String) => String): Map[Symbol, String] =
    try {
      val newToken = refresh(FOREIGN_TYPE, token)
      val result = Await.result(WS.url(URL.ME.format(newToken)).get(), TIMEOUT_DURATION).ahcResponse.getResponseBody
      val (id, nickname, birthday, mail) = (
        Json.parse(result).\("id").as[String],
        Json.parse(result).\("name").as[String],
        Json.parse(result).\("birthday").as[String],
        Json.parse(result).\("email").as[String])

      Log.debug(s"Facebook id: $id, nickname: $nickname, birthday: $birthday")
      Map('id -> id, 'nickname -> nickname, 'birthday -> birthday, 'mail -> mail)
    } catch {
      case e: Exception => if (default.isEmpty) throw e
      else {
        Log.warn(s"Get Facebook me failed.", e)
        default
      }
    }

  /**
   * Get Facebook thumbnail picture URL.
   *
   * @param token Facebook access token.
   * @param default Default picture URL if connect Facebook failed.
   * @param refresh Block of code for refresh and save token if current is expired.
   * @return Facebook thumbnail picture URL.
   */
  def pictureURL(token: String, default: Option[String] = None, refresh: (Int, String) => String = (Int, String) => String): String =
    try {
      val newToken = refresh(FOREIGN_TYPE, token)
      val result = Await.result(WS.url(URL.PICTURE.format(newToken)).get(), TIMEOUT_DURATION).ahcResponse.getResponseBody
      val url = Json.parse(result).\\("url")(0).as[String].replace("q.jpg", "s.jpg")
      Log.debug(s"Facebook picture URL: $url")
      url
    } catch {
      case e: Exception => default match {
        case Some(value) =>
          Log.warn(s"Get Facebook picture failed.", e)
          value
        case _ => throw e
      }
    }

  /**
   * Get Facebook friends amount.
   *
   * @param token Facebook access token.
   * @param default Default friends amount if connect Facebook failed.
   * @param refresh Block of code for refresh and save token if current is expired.
   * @return Facebook friends amount.
   */
  def friendsAmount(token: String, default: Option[Int] = None, refresh: (Int, String) => String = (Int, String) => String): Int =
    try {
      val newToken = refresh(FOREIGN_TYPE, token)
      val result = Await.result(WS.url(URL.FRIENDS.format(newToken)).get(), TIMEOUT_DURATION).ahcResponse.getResponseBody
      val amount = (Json.parse(result) \ "data" \\ "id").size
      Log.debug(s"Facebook friends amount: $amount")
      amount
    } catch {
      case e: Exception => default match {
        case Some(value) =>
          Log.warn(s"Get Facebook friends amount failed.", e)
          value
        case _ => throw e
      }
    }

  /**
   * Publish feed on user wall.
   *
   * @param token Facebook access token.
   * @param message Message to post.
   * @param refresh Block of code for refresh and save token if current is expired.
   * @param expire Block of code for update expired token.
   * @return Feed post result.
   */
  def feed(token: String, message: String,
           refresh: (Int, String) => String = (Int, String) => String,
           expire: (Int, String) => Unit = (Int, String) => Unit): Boolean = {
    val newToken = refresh(FOREIGN_TYPE, token)
    val response = Await.result(WS.url(URL.FEED.format(newToken, URLEncoder.encode(message, "UTF-8"))).post(""), TIMEOUT_DURATION).ahcResponse

    response.getStatusCode match {
      case 200 => true
      case 400 =>
        Log.error(s"Facebook feed failed, token: $token, message: $message, status code: 400, response: ${response.getResponseBody}")
        expire(FOREIGN_TYPE, token)
        false
      case statusCode =>
        Log.error(s"Facebook feed failed, token: $token, message: $message, status code: $statusCode, response: ${response.getResponseBody}")
        false
    }
  }

  /**
   * Publish photo on user wall.
   *
   * @param token Facebook access token.
   * @param file Photo file to post.
   * @param message Message to post for this photo.
   * @param refresh lock of code for refresh and save token if current is expired.
   * @param expire Block of code for update expired token.
   * @return Feed post result.
   */
  def postPhoto(token: String, file: File, message: String,
                refresh: (Int, String) => String = (Int, String) => String,
                expire: (Int, String) => Unit = (Int, String) => Unit): Boolean = {
    val newToken = refresh(FOREIGN_TYPE, token)
    val url = URL.POST_PHOTO.format(newToken)
    val response = HttpUtil.post(url, params = Map('message -> message), fileOpt = Some("source", file))
    val photo = file.getName

    response._1 match {
      case 200 =>
        Log.debug(s"Facebook post photo success, token: $token, message: $message, photo: $photo")
        true
      case 400 =>
        Log.error(s"Facebook post photo failed, token: $token, message: $message, photo: $photo, status code: 400, response: ${response._2}")
        expire(FOREIGN_TYPE, token)
        false
      case statusCode =>
        Log.error(s"Facebook post photo failed, token: $token, message: $message, photo: $photo, status code: $statusCode, response: ${response._2}")
        false
    }
  }

  /**
   * Refresh Facebook access token.
   * It will return None if there is no need to refresh.
   *
   * Response map entries: accessToken, expireDate
   *
   * @param token Facebook access token.
   * @param expireDate Previous token expire time.
   * @return New token and other data after refresh.
   * @throws Exception Refresh token failed.
   */
  def refreshToken(token: String, expireDate: Option[Date]): Option[Map[Symbol, String]] =
    if (expireDate.isEmpty || expireDate.get.before(new Date)) {
      val result = Await.result(WS.url(URL.ACCESS_TOKEN).post(Map(
        "grant_type" -> Seq("fb_exchange_token"),
        "client_id" -> Seq(clientId),
        "client_secret" -> Seq(clientSecret),
        "fb_exchange_token" -> Seq(token))), core.common.Constant.Http.TIMEOUT_DURATION).ahcResponse

      result.getStatusCode match {
        case 200 =>
          result.getResponseBody.split("&") match {
            case Array(token, expireDate) =>
              Log.debug(s"Facebook access token: $token, expire date: $expireDate")
              Some(Map(
                'accessToken -> token.split("=")(1),
                'expireDate -> expireDate.split("=")(1)))
            case _ =>
              Log.error(s"Response body: ${result.getResponseBody}")
              throw new CommonException(s"Refresh Facebook token failed: $token")
          }
        case statusCode =>
          Log.error(s"Status code: $statusCode, response body: ${result.getResponseBody}")
          throw new CommonException(s"Refresh Facebook token failed: $token")
      }
    } else None

}