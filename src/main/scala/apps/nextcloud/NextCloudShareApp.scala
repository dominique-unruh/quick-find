package de.unruh.quickfind
package apps.nextcloud

import apps.nextcloud.NextCloudShareApp.logger

import com.typesafe.scalalogging.Logger
import de.unruh.quickfind.core.DeferredVal

import java.awt.event.KeyEvent
import java.awt.{BorderLayout, Dimension, KeyboardFocusManager}
import javax.swing.{JFrame, JPanel, JScrollPane, JTextArea, WindowConstants}
import scala.compiletime.uninitialized
import scala.concurrent.Promise

class NextCloudShareApp extends JFrame {
  private given DeferredVal.CheckInitManager()
  private val resultArea = DeferredVal[JTextArea]

  println(s"resultArea: $resultArea")

  initialize()

  println(s"resultArea: $resultArea")

  def showApp(): Unit = {
    setVisible(true)
  }

  def initialize(): Unit = {
    setTitle("NextCloud File Share")
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    setPreferredSize(new Dimension(600, 400))


    resultArea := new JTextArea()
    resultArea.setLineWrap(true)
    resultArea.setWrapStyleWord(true)
    resultArea.setRows(15)
    resultArea.setTransferHandler(new UploadingTransferHandler(
      errorMessage = logger.error,
      infoMessage = logger.info,
      pasteString = UploadingTransferHandler.defaultStringPaste(alsoCopy = true, suffix = "\n")
    ))

    val scrollPane = new JScrollPane(resultArea)

    val mainPanel = new JPanel(new BorderLayout())
    mainPanel.add(scrollPane, BorderLayout.CENTER)

    KeyboardFocusManager.getCurrentKeyboardFocusManager.addKeyEventDispatcher((event: KeyEvent) => event.getID match
      case KeyEvent.KEY_PRESSED => event.getKeyCode match
        case KeyEvent.VK_ESCAPE => close(); true
        case KeyEvent.VK_W if event.isControlDown => close(); true
        case KeyEvent.VK_Q if event.isControlDown => close(); true
        case _ => false
      case _ => false)


    setContentPane(mainPanel)
    pack()
    setLocationRelativeTo(null)

    DeferredVal.assertInitialized()
  }

  def close(): Unit = {
    setVisible(false)

  }
}

object NextCloudShareApp {
  def main(args: Array[String]): Unit = {
    val frame = NextCloudShareApp()
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.showApp()
  }

  private val logger = Logger[NextCloudShareApp]
}

