package core.sns

import java.util.Date
import java.net.URLEncoder
import scala.util._
import scala.concurrent._
import play.api.libs.ws._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import core.common._
import core.utils.StringUtil

/**
 * Ameba API utility.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Ameba {
  val FOREIGN_TYPE = 3

  /**
   * Ameba configuration in application.conf
   */
  val API_HOST = AppConfig.get("ca.am.api_host")
  val AUTH_HOST = AppConfig.get("ca.am.auth_host")
  val CLIENT_ID = AppConfig.get("ca.am.client_id")
  val CLIENT_SECRET = AppConfig.get("ca.am.client_secret")

  /**
   * Ameba API urls.
   */
  object URL {
    val REFRESH_TOKEN = s"$AUTH_HOST/token"
    val PROFILE = s"$API_HOST/api/profile/user/getLoginUserProfile/json"
    val PV = s"$API_HOST/api/blogAccessAnalyze/getSummary/json"
    val POST = s"$API_HOST/api/now/postNow/json"
  }

  /**
   * Get Ameba user profile info.
   * Response map entries: nickname, birthday, thumbnailUrl
   *
   * @param token Ameba access token.
   * @param default Default profile info if connect Ameba failed.
   * @param refresh Block of code for refresh and save token if current is expired.
   * @return Ameba user profile info.
   */
  def profile(token: String, default: Map[Symbol, String] = Map(), refresh: (Int, String) => String = (Int, String) => String): Map[Symbol, String] =
    try {
      val newToken = refresh(FOREIGN_TYPE, token)
      val profileResult = Await.result(WS.url(URL.PROFILE).withHeaders(
        ("Content-Type", "application/x-www-form-urlencoded"),
        ("Authorization", "OAuth " + newToken)).get(), core.common.Constant.Http.TIMEOUT_DURATION).ahcResponse.getResponseBody
      Log.debug(s"Profile result: $profileResult")

      val (nickname, birthday, thumbnail) = (
        Json.parse(profileResult).\\("nickname")(0).as[String],
        Json.parse(profileResult).\\("birthday")(0).as[String],
        Json.parse(profileResult).\\("mainPictureUrl")(0).as[String])

      Log.debug(s"Ameba nickname: $nickname, birthday: $birthday, thumbnail: $thumbnail, token: $token")
      Map('nickname -> nickname, 'birthday -> birthday, 'thumbnailUrl -> thumbnail)
    } catch {
      case e: Exception => if (default.isEmpty) throw e
      else {
        Log.warn(s"Get Ameba profile failed.", e)
        default
      }
    }

  /**
   * Get Ameba PV before end date for certain days.
   *
   * @param id Ameba id.
   * @param token Ameba access token.
   * @param endDate End date of PV.
   * @param days How many days for counting.
   * @param default Default PV if connect Ameba failed.
   * @param refresh Block of code for refresh and save token if current is expired.
   * @return Ameba blog PV value.
   * @throws HttpException Token expired.
   */
  def pv(id: String, token: String, endDate: String, days: Int, default: Option[Int] = None,
         refresh: (Int, String) => String = (Int, String) => String): Int =
    try {
      val newToken = refresh(FOREIGN_TYPE, token)
      val result = Await.result(WS.url(URL.PV)
        .withHeaders(("Content-Type", "application/x-www-form-urlencoded"), ("Authorization", "OAuth " + newToken))
        .withQueryString("amebaId" -> id)
        .withQueryString("endDate" -> endDate)
        .withQueryString("day" -> days.toString)
        .get, core.common.Constant.Http.TIMEOUT_DURATION).ahcResponse

      result.getStatusCode match {
        case 401 => throw new HttpException(s"Token expired, id: $id, token: $newToken")
        case _ =>
          val body: String = result.getResponseBody
          ((Json.parse(body) \ "summary" \ "pageviews").as[List[String]] :::
            (Json.parse(body) \ "summary" \ "mobilePageviews").as[List[String]]).map(pv => pv.toInt).sum
      }
    } catch {
      case e: Exception => default match {
        case Some(value) =>
          Log.warn(s"Get Ameba PV failed.", e)
          value
        case _ => throw e
      }
    }

  /**
   * Get Ameba PV before end date for certain days.
   * Here is an example for how to use this function.
   *
   * private def saveAmebaPV(userId: Long)(implicit connection: Connection = null) =
       UserForeignDao.getByUserIdAndForeignType(userId, AuthConstant.ForeignType.AM) match {
         case Some(foreign) =>
           val endDate = DateUtil.date2Str(new Date, DateUtil.Format.YYYYMMDD)
           Ameba.pvAsync(foreign.foreignId, foreign.accessToken, endDate, 30, Some(0))() {
             pv =>
               // because this is a async function, when this function be processed,
               // the outer connection may already been closed.
               // so use an explicit null as connection parameter, force it to create a new connection.
               val social = this.getSocial(userId)(null)
               social.amebaPV = pv
               UserSocialDao.save(social)(null)
               Log.debug(s"Updated Ameba PV in social for user: [$userId].")
           }
         case _ => Log.warn(s"User foreign not found for user [$userId], save Ameba PV failed.")
       }
   *
   * @param id Ameba id.
   * @param token Ameba access token.
   * @param endDate End date of PV.
   * @param days How many days for counting.
   * @param default Default PV if connect Ameba failed.
   * @param onSuccess Logic to be executed after get PV.
   * @param refresh Block of code for refresh and save token if current is expired.
   * @return Ameba blog PV value.
   * @throws HttpException Token expired.
   */
  def pvAsync(id: String, token: String, endDate: String, days: Int, default: Option[Int] = None,
              refresh: (Int, String) => String = (Int, String) => String)(onSuccess: Int => Unit) =
    try {
      val newToken = refresh(FOREIGN_TYPE, token)
      val future: Future[Int] = WS.url(URL.PV)
        .withHeaders(("Content-Type", "application/x-www-form-urlencoded"), ("Authorization", "OAuth " + newToken))
        .withQueryString("amebaId" -> id)
        .withQueryString("endDate" -> endDate)
        .withQueryString("day" -> days.toString)
        .get.map {
        response =>
          ((response.json \ "summary" \ "pageviews").as[List[String]] :::
            (response.json \ "summary" \ "mobilePageviews").as[List[String]]).map(pv => pv.toInt).sum
      }

      future.onComplete {
        case Success(pv) =>
          Log.debug(s"Ameba id: [$id], PV: [$pv], token: [$token].")
          onSuccess(pv)
        case Failure(e) => default match {
          case Some(value) =>
            Log.warn(s"Get Ameba PV failed, using default value: $value.", e)
            onSuccess(value)
          case _ => throw e
        }
      }
    } catch {
      case e: Exception => default match {
        case Some(value) =>
          Log.warn(s"Get Ameba PV failed.", e)
          value
        case _ => throw e
      }
    }

  /**
   * Post message to Ameba blog.
   *
   * @param token Ameba access token.
   * @param message Message to be post.
   * @param refresh Block of code for refresh and save token if current is expired.
   * @return Post result.
   */
  def post(token: String, message: String, refresh: (Int, String) => String = (Int, String) => String): Boolean = {
    val newToken = refresh(FOREIGN_TYPE, token)
    val response = Await.result(WS.url(URL.POST)
      .withHeaders(("Content-Type", "application/x-www-form-urlencoded"), ("Authorization", "OAuth " + newToken))
      .post(Map("entryText" -> Seq(URLEncoder.encode(message, "UTF-8")))), core.common.Constant.Http.TIMEOUT_DURATION).ahcResponse
    response.getStatusCode match {
      case 200 => true
      case statusCode =>
        Log.error(s"Ameba post failed, token: $token, message: $message, status code: $statusCode, response: ${response.getResponseBody}")
        false
    }
  }

  /**
   * Refresh Ameba access token.
   * It will return None if token cannot be refresh or no need to refresh.
   *
   * Response map entries: accessToken, refreshToken, expiresIn
   *
   * @param refreshToken Ameba refresh token.
   * @param expireDate Previous token expire time.
   * @return New token and other data after refresh.
   * @throws HttpException Refresh token failed.
   */
  def refreshToken(refreshToken: String, expireDate: Option[Date]): Option[Map[Symbol, String]] =
    if (StringUtil.isNotBlank(refreshToken) && (expireDate.isEmpty || expireDate.get.before(new Date))) {
      Log.debug(s"Ameba refresh token: $refreshToken")

      val result = Await.result(WS.url(URL.REFRESH_TOKEN).post(Map(
        "grant_type" -> Seq("refresh_token"),
        "client_id" -> Seq(CLIENT_ID),
        "client_secret" -> Seq(CLIENT_SECRET),
        "refresh_token" -> Seq(refreshToken))), core.common.Constant.Http.TIMEOUT_DURATION).ahcResponse

      result.getStatusCode match {
        case 200 =>
          val obj = Json.parse(result.getResponseBody)
          val (accessToken, refreshToken, expiresIn) = (
            obj.\("access_token").as[String],
            obj.\("refresh_token").as[String],
            obj.\("expires_in").as[Int])

          Log.debug(s"Ameba access token: $accessToken, refresh token: $refreshToken, expires in: $expiresIn")
          Some(Map('accessToken -> accessToken, 'refreshToken -> refreshToken, 'expiresIn -> expiresIn.toString))
        case statusCode => throw new HttpException(s"Refresh token failed, status code: $statusCode, refresh token: $refreshToken")
      }
    } else None
}
