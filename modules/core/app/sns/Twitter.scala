package sns

import java.io.File
import java.net.URLEncoder
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HTTP
import org.apache.http.util.EntityUtils

import scala.concurrent.Await
import play.api.libs.oauth._
import play.api.libs.ws.WS
import play.api.libs.json.Json
import core.common._

/**
 * Twitter API utility.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object Twitter {
  val FOREIGN_TYPE = 1

  /**
   * Twitter configuration in application.conf
   */
  val CONSUMER_KEY = AppConfig.get("ca.tw.consumer_key")
  val CONSUMER_SECRET = AppConfig.get("ca.tw.consumer_secret")

  /**
   * Twitter API urls.
   */
  object URL {
    val ACCOUNT = "https://api.twitter.com/1.1/account/verify_credentials.json"
    val TWEET = "https://api.twitter.com/1.1/statuses/update.json?status=%s"
    val POST_PHOTO = "https://api.twitter.com/1.1/statuses/update_with_media.json"
  }

  /**
   * Get Twitter account info.
   * Response map entries: name, screenName, thumbnailUrl, followers
   *
   * @param token Twitter oauth access token.
   * @param secret Twitter oauth token secret.
   * @return Twitter account info.
   */
  def account(token: String, secret: String, default: Map[Symbol, String] = Map()): Map[Symbol, String] =
    try {
      val oauthCalculator = OAuthCalculator(ConsumerKey(CONSUMER_KEY, CONSUMER_SECRET), RequestToken(token, secret))
      val resultBody = Await.result(WS.url(URL.ACCOUNT).sign(oauthCalculator).get(), Constant.Http.TIMEOUT_DURATION).ahcResponse.getResponseBody
      val (name, screenName, thumbnail, followers) = (
        Json.parse(resultBody).\("name").as[String],
        Json.parse(resultBody).\("screen_name").as[String],
        Json.parse(resultBody).\("profile_image_url_https").as[String].replace("image_normal", "image"),
        Json.parse(resultBody).\("followers_count").as[Int])

      Log.debug(s"Twitter name: $name, screen name: $screenName, followers: $followers, thumbnail: $thumbnail")
      Map('name -> name, 'screenName -> screenName, 'thumbnailUrl -> thumbnail, 'followers -> followers.toString)
    } catch {
      case e: Exception => if (default.isEmpty) throw e
      else {
        Log.warn(s"Get Twitter account failed.", e)
        default
      }
    }

  /**
   * Updates the authenticating user's current status, also known as tweeting.
   *
   * @param token Twitter oauth access token.
   * @param secret Twitter oauth token secret.
   * @param message Message for tweeting.
   * @return Tweet result.
   */
  def tweet(token: String, secret: String, message: String): Boolean = {
    val oauthCalculator = OAuthCalculator(ConsumerKey(CONSUMER_KEY, CONSUMER_SECRET), RequestToken(token, secret))
    val response = Await.result(WS.url(URL.TWEET.format(URLEncoder.encode(message, "UTF-8"))).sign(oauthCalculator).post(""),
      Constant.Http.TIMEOUT_DURATION).ahcResponse
    response.getStatusCode match {
      case 200 => true
      case statusCode =>
        Log.error(s"Twitter tweet failed, token: $token, message: $message, status code: $statusCode, response: ${response.getResponseBody}")
        false
    }
  }

  /**
   * Updates the authenticating user's current status and attaches media for upload.
   *
   * @param token Twitter oauth access token.
   * @param secret Twitter oauth token secret.
   * @param file Photo file to post.
   * @param message Message for tweeting.
   * @return Tweet result.
   */
  def tweetPhoto(token: String, secret: String, file: File, message: String): Boolean = {
    val httpPost = new HttpPost(URL.POST_PHOTO)
    val oAuthConsumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET)
    oAuthConsumer.setTokenWithSecret(token, secret)
    oAuthConsumer.sign(httpPost)

    val builder = MultipartEntityBuilder.create
    builder.addTextBody("status", message, ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8))
    builder.addPart("media[]", new FileBody(file))
    httpPost.setEntity(builder.build)

    Log.debug("HTTP POST: " + httpPost.getURI)
    val response = HttpClientBuilder.create.build.execute(httpPost)
    Log.debug("Response code: " + response.getStatusLine)
    Log.debug("Response entity: " + response.getEntity.getContentType)

    response.getStatusLine.getStatusCode match {
      case 200 => true
      case statusCode =>
        Log.error(s"Twitter tweet failed, token: $token, message: $message, status code: $statusCode, response: ${EntityUtils.toString(response.getEntity)}")
        false
    }
  }
}
