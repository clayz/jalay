package core.utils

import java.io._
import java.net._
import scala.io.Source
import scala.sys.process._
import core.common.Log

/**
 * File operation utilities.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object FileUtil {
  /**
   * Check whether file exists.
   *
   * @param path Target file path.
   * @return Boolean File exists or not.
   */
  def exist(path: String): Boolean = new File(path).exists

  /**
   * Delete file from file system.
   *
   * @param path Target file path.
   * @return Boolean Delete file result.
   */
  def delete(path: String): Boolean = {
    val file = new File(path)
    if (file.exists) file.delete else false
  }

  /**
   * Read simple text content from file.
   *
   * @param path Absolute path of target file.
   * @return String File content.
   */
  def read(path: String): String = {
    val content = new StringBuilder
    for (line <- Source.fromFile(path).getLines) content.append(line).append("\n")
    content.toString
  }

  /**
   * Read simple text content from file.
   *
   * @param file Text file.
   * @return String File content.
   */
  def read(file: File): String = {
    val content = new StringBuilder
    for (line <- Source.fromFile(file).getLines) content.append(line).append("\n")
    content.toString
  }

  /**
   * Read byte array data from file.
   *
   * @param path Absolute path of target file.
   * @return Array[Byte] File content.
   */
  def readByte(path: String): Array[Byte] = {
    var in: InputStream = null
    var data: Array[Byte] = null

    try {
      in = new FileInputStream(path)
      data = new Array[Byte](in.available)
      in.read(data)
      data
    } catch {
      case e: IOException =>
        Log.error("Read byte array form file failed, path: " + path, e)
        throw e
    } finally {
      if (in != null) in.close
    }
  }

  /**
   * Read byte array data from internet by url.
   *
   * @param url File url address.
   * @return Array[Byte] File content.
   */
  def readByte(url: URL): Array[Byte] = {
    var in: InputStream = null
    var out: ByteArrayOutputStream = null

    try {
      in = url.openStream
      out = new ByteArrayOutputStream
      val tmp = new Array[Byte](1024)
      var read = 0
      while ( {
        read = in.read(tmp)
        read != -1
      }) out.write(tmp, 0, read)
      out.toByteArray
    } catch {
      case e: IOException =>
        Log.error("Read byte array form internet failed, URL: " + url.toString, e)
        throw e
    } finally {
      if (out != null) out.close
      if (in != null) in.close
    }
  }

  /**
   * Write byte array data into file.
   *
   * @param path Target file path, include file name.
   * @param data File content.
   */
  def write(path: String, data: Array[Byte]) {
    var out: OutputStream = null

    try {
      out = new FileOutputStream(path)
      out.write(data)
      out.flush
    } catch {
      case e: IOException =>
        Log.error("Write byte array to file failed, path: " + path, e)
        throw e
    } finally {
      if (out != null) out.close
    }
  }

  /**
   * Transfer file from local to other servers by SCP.
   *
   * @param sourcePath File path on local machine.
   * @param destinationPath File path on destination machine.
   * @param destinations Destination IP address list.
   * @param user Login user for all destination machine.
   */
  def copyToRemote(sourcePath: String, destinationPath: String, destinations: Array[String], user: String) = {
    destinations.foreach(destination => {
      Log.info("Send file from %s to destination %s:%s.".format(sourcePath, destination, destinationPath))
      val result = {
        "scp " + sourcePath + " " + user + "@" + destination + ":" + destinationPath
      }.!
      Log.info("Send file command result: " + result.toString)
    })
  }
}