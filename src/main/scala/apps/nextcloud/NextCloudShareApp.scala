package de.unruh.quickfind
package apps.nextcloud

import apps.calendar.{ImageTransferable, StringTransferable}
import apps.calendar.FileTransferable

import com.typesafe.scalalogging.Logger
import org.aarboard.nextcloud.api.NextcloudConnector
import org.aarboard.nextcloud.api.filesharing.{SharePermissions, ShareType}
import org.apache.commons.io.FilenameUtils

import java.awt.*
import java.awt.datatransfer.*
import java.awt.image.BufferedImage
import java.io.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.text.JTextComponent
import scala.io.Source
import scala.util.{Random, Using}

object NextCloudShareApp {


  def main(args: Array[String]): Unit = {
    createAndShowGUI()
  }

  def createAndShowGUI(): Unit = {
    val frame = new JFrame("NextCloud File Share")
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.setPreferredSize(new Dimension(600, 400))


    val resultArea = new JTextArea()
    //    resultArea.setEditable(false)
    resultArea.setLineWrap(true)
    resultArea.setWrapStyleWord(true)
    resultArea.setRows(15)
    resultArea.setTransferHandler(new UploadingTransferHandler(
      errorMessage = logger.error,
      infoMessage = logger.info,
    ))

    val scrollPane = new JScrollPane(resultArea)

    val mainPanel = new JPanel(new BorderLayout())
    mainPanel.add(scrollPane, BorderLayout.CENTER)

    frame.setContentPane(mainPanel)
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.setVisible(true)
  }

/*  def handlePaste(): Unit = {
    val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
    val contents = clipboard.getContents(null)

    if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      logger.debug("Processing pasted image...")
      val image = contents.getTransferData(DataFlavor.imageFlavor).asInstanceOf[BufferedImage]

      // Save image to temp file
      val timestamp = System.currentTimeMillis()
      val tempFile = File.createTempFile(s"pasted_$timestamp", ".png")
      ImageIO.write(image, "png", tempFile)

      Nextcloud.fileToUrl(tempFile, errorMessage = logger.error)
      tempFile.delete()
    } else {
      logger.debug("No image found in clipboard")
    }
  }*/


  private val logger = Logger[NextCloudShareApp.type]
}

/*
  class UploadingDropTarget(errorMessage: String => Unit,
                            infoMessage: String => Unit,
                            pasteString: String => Unit) extends DropTargetAdapter {
    override def dragOver(dtde: DropTargetDragEvent): Unit = {
      dtde.acceptDrag(DnDConstants.ACTION_COPY)
    }

    override def drop(dtde: DropTargetDropEvent): Unit = {
      dtde.acceptDrop(DnDConstants.ACTION_COPY)
      val transferable = dtde.getTransferable

      val stringToPaste = transferable match {
        case FileTransferable(files*) =>
          files.map(processFile(_, errorMessage)).mkString(", ")
        case StringTransferable(string) =>
          string
      }

      println((stringToPaste, stringToPaste.getClass))

      pasteString(stringToPaste)
    }
  }
*/

/*  class FileTransferHandler(statusLabel: JLabel, resultArea: JTextArea) extends TransferHandler {
    override def canImport(support: TransferHandler.TransferSupport): Boolean = {
      println(support)

      support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
    }

    override def importData(support: TransferHandler.TransferSupport): Boolean = {
      if (!canImport(support)) return false

      try {
        val transferable = support.getTransferable
        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor)
          .asInstanceOf[java.util.List[File]]

        import scala.jdk.CollectionConverters._
        files.asScala.foreach { file =>
          if (file.isFile) {
            processFile(file, statusLabel, resultArea)
          }
        }
        true
      } catch {
        case e: Exception =>
          statusLabel.setText("Error during drop")
          resultArea.setText(resultArea.getText + s"\nError: ${e.getMessage}\n")
          e.printStackTrace()
          false
      }
    }
  }*/
