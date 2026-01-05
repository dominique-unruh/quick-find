package de.unruh.quickfind
package apps.nextcloud

import apps.calendar.{FileTransferable, ImageTransferable, StringTransferable}
import apps.nextcloud.UploadingTransferHandler.{defaultStringPaste, logger}

import com.typesafe.scalalogging.Logger
import de.unruh.quickfind.core.Utils

import java.awt.Color
import java.awt.datatransfer.Transferable
import java.io.File
import javax.imageio.ImageIO
import javax.swing.text.JTextComponent
import javax.swing.{JComponent, TransferHandler}
import scala.util.control.NonFatal

class UploadingTransferHandler(errorMessage: String => Unit,
                               infoMessage: String => Unit,
                               pasteString: (JComponent, String) => Unit = defaultStringPaste(),
                               /** Called before the paste starts (can initiate a visual busy indicator, for example). Doesn't seem to work well because color updates are done only when the import is over anyway. */
                               beforePaste: JComponent => Unit = defaultBeforePaste,
                               /** Called after the paste ends (can remove a visual busy indicator, for example) */
                               afterPaste: JComponent => Unit = defaultAfterPaste,
                              ) extends TransferHandler {
  override def canImport(support: TransferHandler.TransferSupport): Boolean = true

  override def importData(component: JComponent, transferable: Transferable): Boolean = {
    val textComponent = component.asInstanceOf[JTextComponent]

    try
      beforePaste(component)
    catch
      case NonFatal(e) => errorMessage(e.getMessage)

    val stringToPaste = try {
      transferable match {
        case FileTransferable(files*) =>
          files.map(Nextcloud.fileToUrl(_, errorMessage)).mkString(", ")
        case StringTransferable(string) =>
          string
        case ImageTransferable(image) =>
          val timestamp = System.currentTimeMillis()
          val tempFile = File.createTempFile(s"pasted_$timestamp", ".png")
          try {
            ImageIO.write(image, "png", tempFile)
            Nextcloud.fileToUrl(tempFile, errorMessage = logger.error)
          } finally
            tempFile.delete()
        case _ =>
          logger.debug(s"Can't handle clipboard content: $transferable")
          "\uD83E\uDD37"
      }
    } catch
      case NonFatal(e) =>
        errorMessage(e.getMessage)
        return false

    pasteString(component, stringToPaste)

    try
      afterPaste(component)
    catch
      case NonFatal(e) => errorMessage(e.getMessage)

    true
  }
}

object UploadingTransferHandler {
  def defaultStringPaste(suffix: String = " ", alsoCopy: Boolean = false)(component: JComponent, string: String): Unit = {
    if (alsoCopy)
      try
        Utils.copyToClipboard(string)
      catch
        case NonFatal(e) => e.printStackTrace()

    component.asInstanceOf[JTextComponent].replaceSelection(string + suffix)
  }

  def defaultBeforePaste(component: JComponent): Unit = {
    component.setBackground(Color.LIGHT_GRAY)
  }

  def defaultAfterPaste(component: JComponent): Unit = {
    component.setBackground(Color.WHITE)
  }

  private val logger = Logger[UploadingTransferHandler]
}