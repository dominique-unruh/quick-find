package de.unruh.quickfind
package core

import org.apache.batik.transcoder.{SVGAbstractTranscoder, TranscoderInput, TranscoderOutput}
import org.apache.batik.transcoder.image.{ImageTranscoder, PNGTranscoder}

import java.awt.{Color, Image}
import java.awt.image.BufferedImage
import java.io.{File, FileInputStream, FileReader, InputStream, InputStreamReader, Reader}
import java.net.URL
import java.nio.file.Path

trait ScalableImage {
  def getImageAtSize(width: Int, height: Int): Image
}

class SVGImage private (name: String, source: () => InputStream) extends ScalableImage {
  private var height: Int = -1
  private var width: Int = -1
  private var image: BufferedImage = _

  override def toString: String = s"[Image $name]"

  def this(source: File) = this(source.toString, () => new FileInputStream(source))
  def this(source: Path) = this(source.toFile)
  def this(source: URL) = this(source.toString, () => source.openStream())

  override def getImageAtSize(width: Int, height: Int): Image = synchronized {
    if (this.height != height || this.width != width) {
      println(s"Rescaling $this to ${width}x$height")
      val transcoderInput = new TranscoderInput(source())
      val imageTranscoder = new ImageTranscoder:
        override def createImage(width: Int, height: Int): BufferedImage =
          image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
          image
        override def writeImage(img: BufferedImage, output: TranscoderOutput): Unit = {}
      imageTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, width.toFloat)
      imageTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, height.toFloat)
//      imageTranscoder.addTranscodingHint(ImageTranscoder.KEY_FORCE_TRANSPARENT_WHITE, true)
      imageTranscoder.transcode(transcoderInput, null)
      this.width = width
      this.height = height
    }
    image
  }
}

object SVGImage {
  def fromResource(resource: String): SVGImage = SVGImage(resource, () => getClass.getResource(resource).openStream())
}