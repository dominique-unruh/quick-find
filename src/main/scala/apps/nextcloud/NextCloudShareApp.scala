package de.unruh.quickfind
package apps.nextcloud

import apps.nextcloud.NextCloudShareApp.logger

import com.typesafe.scalalogging.Logger

import java.awt.{BorderLayout, Dimension}
import javax.swing.{JFrame, JPanel, JScrollPane, JTextArea, WindowConstants}

class NextCloudShareApp extends JFrame {
  initialize()

  def showApp(): Unit = {
    setVisible(true)
  }

  def initialize(): Unit = {
    setTitle("NextCloud File Share")
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    setPreferredSize(new Dimension(600, 400))


    val resultArea = new JTextArea()
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

    setContentPane(mainPanel)
    pack()
    setLocationRelativeTo(null)
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

