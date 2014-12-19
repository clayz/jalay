package core.utils

import java.io._
import java.awt._
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.imageio.ImageIO
import java.net.URL
import core.common.Log
import javax.imageio.ImageWriteParam
import javax.imageio.IIOImage

/**
 * Utilities for image process.
 *
 * @author Clay Zhong
 * @version 1.0.0
 */
object ImageUtil {
  /**
   * Zoom image for new width, height and format, then save to new file.
   *
   * @param imageFile Original image file.
   * @param newPath New image file path.
   * @param width Width of zoom to.
   * @param height Height of zoom to.
   * @param formatType Image type of format.
   */
  def zoom(imageFile: File, newPath: String, width: Int, height: Int, formatType: String): Unit = {
    this.zoom(ImageIO.read(imageFile), newPath, width, height, formatType)
  }

  /**
   * Zoom image for new width, height and format, then save to new file.
   *
   * @param orgPath Original image file path.
   * @param newPath New image file path.
   * @param width Width of zoom to.
   * @param height Height of zoom to.
   * @param formatType Image type of format.
   */
  def zoom(orgPath: String, newPath: String, width: Int, height: Int, formatType: String): Unit = {
    this.zoom(new File(orgPath), newPath, width, height, formatType)
  }

  /**
   * Retrieve file from specified url, zoom image for new width, height and format, then save to new file.
   *
   * @param url Image URL address.
   * @param newPath New image file path.
   * @param width Width of zoom to.
   * @param height Height of zoom to.
   * @param formatType Image type of format.
   */
  def zoom(url: URL, newPath: String, width: Int, height: Int, formatType: String): Unit = {
    this.zoom(ImageIO.read(url), newPath, width, height, formatType)
  }

  /**
   * Retrieve file from specified url, then zoom image to square with new size and format.
   *
   * @param url Image URL address.
   * @param size New image max horizontal or vertical size.
   * @param formatType Image type of format.
   * @return Array[Byte] Image data.
   */
  def zoom(url: URL, size: Int, formatType: String): Array[Byte] = {
    this.zoom(ImageIO.read(url), size, formatType)
  }

  /**
   * Retrieve file from specified url, zoom image and return as byte array data.
   *
   * @param url Image URL address.
   * @param width Width of zoom to.
   * @param height Height of zoom to.
   * @param formatType Image type of format.
   * @return Array[Byte] Image data.
   */
  def zoom(url: URL, width: Int, height: Int, formatType: String): Array[Byte] = {
    this.zoom(ImageIO.read(url), width, height, formatType)
  }

  /**
   * Retrieve file from specified url, zoom image to square and return as byte array data.
   *
   * @param file Target image file.
   * @param size New image max horizontal or vertical size.
   * @param formatType Image type of format.
   * @return Array[Byte] Image data.
   */
  def zoom(file: File, size: Int, formatType: String): Array[Byte] = {
    this.zoom(ImageIO.read(file), size, formatType)
  }

  /**
   * Retrieve file from specified url, zoom image to square and return as byte array data.
   *
   * @param file Target image file.
   * @param size New image max horizontal or vertical size.
   * @param formatType Image type of format.
   * @return Array[Byte] Image data.
   */
  def zoom(file: File, size: Int, formatType: String, crop: (Int, Int, Int, Int)): Array[Byte] = {
    this.zoom(ImageIO.read(file), size, formatType, crop)
  }

  /**
   * Zoom image file and return as byte array data.
   *
   * @param file Target image file.
   * @param width Width of zoom to.
   * @param height Height of zoom to.
   * @param formatType Image type of format.
   * @return Array[Byte] Image data.
   */
  def zoom(file: File, width: Int, height: Int, formatType: String): Array[Byte] = {
    this.zoom(ImageIO.read(file), width, height, formatType)
  }

  /**
   * Zoom image with specified size and format.
   *
   * @param image Target image file buffer.
   * @param size Max width or height after zoom.
   * @param formatType Image type of format.
   * @return Array[Byte] Image data.
   */
  private def zoom(image: BufferedImage, size: Int, formatType: String): Array[Byte] = {
    this.zoom(image, size, formatType, (0, 0, 0, 0))
  }

  /**
   * Zoom image with specified size and format.
   *
   * @param image Target image file buffer.
   * @param size Max width or height after zoom.
   * @param formatType Image type of format.
   * @return Array[Byte] Image data.
   */
  private def zoom(image: BufferedImage, size: Int, formatType: String, crop: (Int, Int, Int, Int)): Array[Byte] = {
    var graphics: Graphics = null
    var outputStream: ByteArrayOutputStream = null

    try {
      // adjust zoom size
      val (originalWidth, originalHeight) = if ((crop._3 != 0) && (crop._4 != 0)) (crop._3, crop._4) else (image.getWidth, image.getHeight)
      var width, height = size

      if (size == -1) {
        width = originalWidth
        height = originalHeight
      } else {
        if (originalWidth > originalHeight) {
          height = (size * (originalHeight.toFloat / originalWidth)).toInt
        } else if (originalWidth < originalHeight) {
          width = (size * (originalWidth.toFloat / originalHeight)).toInt
        }
      }

      Log.debug(s"Image zoom size created.[width=$width][heigth=$height]")

      // create zoom image
      val targetImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      graphics = targetImage.getGraphics

      if ((crop._3 != 0) && (crop._4 != 0))
        graphics.drawImage(new ImageIcon(image.getSubimage(crop._1, crop._2, crop._3, crop._4).getScaledInstance(
          width, height, Image.SCALE_SMOOTH)).getImage, 0, 0, null)
      else
        graphics.drawImage(new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH)).getImage, 0, 0, null)

      outputStream = new ByteArrayOutputStream
      ImageIO.write(targetImage, formatType, outputStream)
      outputStream.flush
      outputStream.toByteArray
    } finally {
      if (graphics != null) graphics.dispose
      if (outputStream != null) outputStream.close
    }
  }

  /**
   * Zoom image with specified width / height and format.
   *
   * @param image Target image file buffer.
   * @param width Specified image zoom width.
   * @param height Specified image zoom height.
   * @param formatType Image type of format.
   * @return Array[Byte] Image data.
   */
  private def zoom(image: BufferedImage, width: Int, height: Int, formatType: String): Array[Byte] = {
    var graphics: Graphics = null
    var outputStream: ByteArrayOutputStream = null

    try {
      // create zoom image
      val targetImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      graphics = targetImage.getGraphics
      graphics.drawImage(new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH)).getImage, 0, 0, null)

      outputStream = new ByteArrayOutputStream
      ImageIO.write(targetImage, formatType, outputStream)
      outputStream.flush
      outputStream.toByteArray
    } finally {
      if (graphics != null) graphics.dispose
      if (outputStream != null) outputStream.close
    }
  }

  /**
   * Zoom image with specified width / height and format, then save into new file.
   *
   * @param image Target image file buffer.
   * @param newPath Generated file path.
   * @param width Specified image zoom width.
   * @param height Specified image zoom height.
   * @param formatType Image type of format.
   */
  private def zoom(image: BufferedImage, newPath: String, width: Int, height: Int, formatType: String): Unit = {
    var graphics: Graphics = null

    try {
      val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      graphics = bufferedImage.getGraphics
      graphics.drawImage(new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH)).getImage, 0, 0, null)

      ImageIO.write(bufferedImage, formatType, new File(newPath))
    } finally {
      if (graphics != null) graphics.dispose
    }
  }
}