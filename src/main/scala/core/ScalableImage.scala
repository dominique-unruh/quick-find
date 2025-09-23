package de.unruh.quickfind
package core

import com.googlecode.jfilechooserbookmarks.DefaultIconLoader
import de.unruh.quickfind.core.SVGImage.createPlaceholderImage
import org.apache.batik.transcoder.{SVGAbstractTranscoder, TranscoderInput, TranscoderOutput}
import org.apache.batik.transcoder.image.{ImageTranscoder, PNGTranscoder}

import java.awt.{BasicStroke, Color, Font, Image, RenderingHints}
import java.awt.image.BufferedImage
import java.io.{File, FileInputStream, FileReader, InputStream, InputStreamReader, Reader}
import java.net.URL
import java.nio.file.Path
import javax.swing.Icon
import scala.compiletime.uninitialized
import scala.util.control.NonFatal

trait ScalableImage {
  def getImageAtSize(width: Int, height: Int): Image
}

/*
object DummyImage extends ScalableImage {
  override def getImageAtSize(width: Int, height: Int): Image =
    new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
}
*/

class SVGImage private (name: String, source: () => InputStream) extends ScalableImage {
  private var height: Int = -1
  private var width: Int = -1
  private var image: Image = uninitialized

  override def toString: String = s"[Image $name]"

  def this(source: File) = this(source.toString, () => new FileInputStream(source))
  def this(source: Path) = this(source.toFile)
  def this(source: URL) = this(source.toString, () => source.openStream())

  override def getImageAtSize(width: Int, height: Int): Image = synchronized {
    try {
      if (this.height != height || this.width != width) {
        println(s"Rescaling $this to ${width}x$height")
        val transcoderInput = new TranscoderInput(source())
        val imageTranscoder = new ImageTranscoder {
          override def createImage(width: Int, height: Int): BufferedImage =
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            SVGImage.this.image = image
            image

          override def writeImage(img: BufferedImage, output: TranscoderOutput): Unit = {}
        }
        imageTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, width.toFloat)
        imageTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, height.toFloat)
        //      imageTranscoder.addTranscodingHint(ImageTranscoder.KEY_FORCE_TRANSPARENT_WHITE, true)
        imageTranscoder.transcode(transcoderInput, null)
        this.width = width
        this.height = height
      }
      image
    } catch {
      case NonFatal(e) =>
        println(s"Transcoding image $name failed: $e")
        this.image = createPlaceholderImage(width, height)
        image
    }
  }
}

object SVGImage {
  def fromResource(resource: String): SVGImage =
    val resourceURL = getClass.getResource(resource)
    assert(resourceURL != null, resource)
    SVGImage(resource, () => resourceURL.openStream())


  /**
   * Creates a placeholder image with a border and "No Image" text.
   */
  private def createPlaceholderImage(width: Int, height: Int,
                             backgroundColor: Color = Color.WHITE,
                             borderColor: Color = Color.GRAY,
                             textColor: Color = Color.DARK_GRAY): Image = {
    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g2d = image.createGraphics()

    // Enable antialiasing for better text rendering
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    // Fill background
    g2d.setColor(backgroundColor)
    g2d.fillRect(0, 0, width, height)

    // Draw border
    g2d.setColor(borderColor)
    g2d.setStroke(new BasicStroke(2))
    g2d.drawRect(1, 1, width - 2, height - 2)

    // Draw diagonal lines (classic "broken image" pattern)
    g2d.setStroke(new BasicStroke(1))
    g2d.drawLine(0, 0, width, height)
    g2d.drawLine(width, 0, 0, height)

    // Add text
    g2d.setColor(textColor)
    val font = new Font(Font.SANS_SERIF, Font.BOLD, math.min(width, height) / 8)
    g2d.setFont(font)

    val text = "SVG error"
    val fontMetrics = g2d.getFontMetrics
    val textBounds = fontMetrics.getStringBounds(text, g2d)
    val textX = (width - textBounds.getWidth) / 2
    val textY = (height - textBounds.getHeight) / 2 + fontMetrics.getAscent

    g2d.drawString(text, textX.toInt, textY.toInt)
    g2d.dispose()

    image
  }
}