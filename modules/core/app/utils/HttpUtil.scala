package core.utils

import java.io.File
import java.util
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.entity.ContentType
import org.apache.http.protocol.HTTP

import scalax.io.Resource
import org.apache.http.entity.mime._
import org.apache.http.NameValuePair
import org.apache.http.entity.mime.content._
import org.apache.http.client.methods._
import org.apache.http.impl.client._
import org.apache.http.util.EntityUtils
import org.apache.http.HttpResponse
import org.apache.http.message.BasicNameValuePair
import core.common._

/**
 * Http Util.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object HttpUtil {
  /**
   * Execute HTTP GET request and retrieve response data.
   *
   * @param url Target URL address.
   * @param params HTTP GET parameters.
   * @param headers HTTP headers.
   * @return String Retrieved response data.
   */
  def get(url: String, params: Map[Symbol, String] = Map(), headers: Map[Symbol, String] = Map()): String = {
    val response = this.httpGet(url, params, headers)
    EntityUtils.toString(response.getEntity)
  }

  /**
   * Execute HTTP GET request, retrieve and save response file.
   *
   * @param filePath Path to save retrieved file.
   * @param url Target URL address.
   * @param params HTTP GET parameters.
   * @param headers HTTP headers.
   * @return File Retrieved response file.
   */
  def getFile(filePath: String, url: String, params: Map[Symbol, String] = Map(), headers: Map[Symbol, String] = Map()): File = {
    FileUtil.write(filePath, this.getFileContent(url, params, headers))
    new File(filePath)
  }

  /**
   * Execute HTTP GET request and retrieve response stream data.
   *
   * @param url Target URL address.
   * @param params HTTP GET parameters.
   * @param headers HTTP headers.
   * @return Array[Byte] Retrieved response stream data.
   */
  def getFileContent(url: String, params: Map[Symbol, String] = Map(), headers: Map[Symbol, String] = Map()): Array[Byte] = {
    val response = this.httpGet(url, params, headers)
    Resource.fromInputStream(response.getEntity.getContent).byteArray
  }

  /**
   * Execute HTTP GET request.
   *
   * @param url Target URL address.
   * @param params HTTP GET parameters.
   * @param headers HTTP headers.
   * @return HttpResponse Remote response.
   */
  private def httpGet(url: String, params: Map[Symbol, String] = Map(), headers: Map[Symbol, String] = Map()): HttpResponse = {
    val httpGet = new HttpGet(url)

    // add headers and parameters
    params.map(param => httpGet.getParams.setParameter(param._1.name, param._2))
    headers.map(header => httpGet.addHeader(header._1.name, header._2))
    Log.debug("HTTP GET: " + httpGet.getURI)

    val response = new DefaultHttpClient().execute(httpGet)
    Log.debug("Response code: " + response.getStatusLine)
    Log.debug("Response entity: " + response.getEntity.getContentType)

    response
  }

  /**
   * Execute HTTP POST request and retrieve response data.
   *
   * @param url Target URL address.
   * @param params HTTP GET parameters.
   * @param headers HTTP headers.
   * @return String Retrieved response data.
   */
  def post(url: String, params: Map[Symbol, String] = Map(), headers: Map[Symbol, String] = Map(), fileOpt: Option[(String, File)] = None): (Int, String) = {
    val response = this.httpPost(url, params, headers, fileOpt)
    (response.getStatusLine.getStatusCode, EntityUtils.toString(response.getEntity))
  }

  /**
   * Execute HTTP POST request.
   *
   * @param url Target URL address.
   * @param params HTTP GET parameters.
   * @param headers HTTP headers.
   * @return HttpResponse Remote response.
   */
  private def httpPost(url: String, params: Map[Symbol, String] = Map(), headers: Map[Symbol, String] = Map(), fileOpt: Option[(String, File)]): HttpResponse = {
    val httpPost = new HttpPost(url)

    fileOpt match {
      case Some((name, file)) =>
        val builder = MultipartEntityBuilder.create
        params.map(param => builder.addTextBody(param._1.name, param._2, ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8)))
        builder.addPart(name, new FileBody(file))
        httpPost.setEntity(builder.build)
      case _ =>
        val nameValuePairs = new util.ArrayList[NameValuePair]
        params.map(param => nameValuePairs.add(new BasicNameValuePair(param._1.name, param._2)))
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs))
        headers.map(header => httpPost.addHeader(header._1.name, header._2))
    }

    Log.debug("HTTP POST: " + httpPost.getURI)
    val response = HttpClientBuilder.create.build.execute(httpPost)
    Log.debug("Response code: " + response.getStatusLine)
    Log.debug("Response entity: " + response.getEntity.getContentType)

    response
  }
}