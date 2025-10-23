package de.unruh.quickfind
package apps.calendar

import java.awt.{BorderLayout, Color, Component, Dimension, FlowLayout, Font, GridBagConstraints, GridBagLayout, Insets}
import java.io.{BufferedReader, File, FileInputStream, InputStream, InputStreamReader, Reader}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.Logger
import de.unruh.quickfind.core.Utils

import java.awt.dnd.*
import java.awt.{BorderLayout, Color, Component, Dimension, FlowLayout, Font, GridBagConstraints, GridBagLayout, Insets}
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{BodyPart, Multipart, Session}
import javax.swing.*
import scala.collection.mutable.ArrayBuffer
import scala.compiletime.uninitialized
import scala.concurrent.Future
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

// Main Application
class AddCalendarEvent extends JFrame {
  private val events = ArrayBuffer[CalendarEvent]()
  private val calendars = Array("private", "work", "family", "other")
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  private var eventsPanel: JPanel = uninitialized
  private var scrollPane: JScrollPane = uninitialized

  initialize()

  private def initialize(): Unit = {
    setTitle("Add calendar events")
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    setSize(900, 600)
    setLayout(new BorderLayout())

    // Create toolbar
    val toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT))
    val newButton = new JButton("New Event")
    newButton.addActionListener { _ =>
      val event = CalendarEvent()
      events += event
      refreshEventsList()
    }

    toolbar.add(newButton)

    eventsPanel = new JPanel()
    eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS))
    eventsPanel.setBackground(Color.WHITE)

    scrollPane = new JScrollPane(eventsPanel)
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    scrollPane.getVerticalScrollBar.setUnitIncrement(16)

    // Set up drag and drop
    new DropTarget(scrollPane, dropTargetAdapter)

    add(toolbar, BorderLayout.NORTH)
    add(scrollPane, BorderLayout.CENTER)
    setLocationRelativeTo(null)
    setVisible(true)

    refreshEventsList()
  }

  def showApp(): Unit = {
    setVisible(true)
  }

  private def refreshEventsList(): Unit = {
    eventsPanel.removeAll()

    events.foreach { event =>
      val eventWidget = createEventWidget(event)
      eventsPanel.add(eventWidget)
      eventsPanel.add(Box.createRigidArea(new Dimension(0, 10)))
    }

    eventsPanel.revalidate()
    eventsPanel.repaint()
  }

  private object dropTargetAdapter extends DropTargetAdapter {
    override def dragOver(dtde: DropTargetDragEvent): Unit = {
      dtde.acceptDrag(DnDConstants.ACTION_COPY)
    }

    override def drop(dtde: DropTargetDropEvent): Unit = {
      try {
        dtde.acceptDrop(DnDConstants.ACTION_COPY)
        val transferable = dtde.getTransferable

        transferable match {
          case FileTransferable(file) if Utils.firstLine(file).exists(_.startsWith("BEGIN:VCALENDAR")) =>
            events ++= ICS.parseICSFile(file)
            refreshEventsList()
            dtde.dropComplete(true)
          case FileTransferable(file) if file.getName.toLowerCase.endsWith(".eml") =>
            events ++= Email.parseEmailFile(file)
            refreshEventsList()
            dtde.dropComplete(true)
          case StringTransferable(content) if content.startsWith("BEGIN:VCALENDAR") =>
            events ++= ICS.parseICSContent(content)
            refreshEventsList()
            dtde.dropComplete(true)
          case StringTransferable(content) =>
            events ++= Email.parseEmailContent(content)
            refreshEventsList()
            dtde.dropComplete(true)
          case _ =>
            showError("Can process this drag and drop object")
            refreshEventsList()
            dtde.dropComplete(false)
        }
      } catch
        case NonFatal(e) =>
          showError(s"Failed to parse dropped event: $e")
    }
  }

  private def createEventWidget(event: CalendarEvent): JPanel = {
    val panel = new JPanel()
    panel.setLayout(new BorderLayout(10, 10))
    panel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
      BorderFactory.createEmptyBorder(15, 15, 15, 15)
    ))
    panel.setBackground(new Color(250, 250, 250))
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200))

    // Left side: Event details
    val detailsPanel = new JPanel(new GridBagLayout())
    detailsPanel.setOpaque(false)
    val gbc = new GridBagConstraints()
    gbc.insets = new Insets(3, 5, 3, 5)
    gbc.anchor = GridBagConstraints.WEST
    gbc.fill = GridBagConstraints.HORIZONTAL

    // Title field (larger)
    val titleField = new JTextField(event.title, 30)
    titleField.setFont(titleField.getFont.deriveFont(Font.BOLD, 14f))
    titleField.getDocument.addDocumentListener(new javax.swing.event.DocumentListener {
      def changedUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def removeUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def insertUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def update(): Unit = event.title = titleField.getText
    })

    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    detailsPanel.add(new JLabel("Title:"), gbc)
    gbc.gridx = 1
    gbc.gridwidth = 3
    gbc.weightx = 1.0
    detailsPanel.add(titleField, gbc)

    // Date/Time fields
    val startField = new JTextField(event.startTime.format(dateTimeFormatter), 15)
    startField.addActionListener { _ =>
      Try(LocalDateTime.parse(startField.getText, dateTimeFormatter)) match {
        case Success(dt) => event.startTime = dt
        case Failure(_) => startField.setText(event.startTime.format(dateTimeFormatter))
      }
    }
    startField.addFocusListener(new java.awt.event.FocusAdapter {
      override def focusLost(e: java.awt.event.FocusEvent): Unit = {
        Try(LocalDateTime.parse(startField.getText, dateTimeFormatter)) match {
          case Success(dt) => event.startTime = dt
          case Failure(_) => startField.setText(event.startTime.format(dateTimeFormatter))
        }
      }
    })

    val endField = new JTextField(event.endTime.format(dateTimeFormatter), 15)
    endField.addActionListener { _ =>
      Try(LocalDateTime.parse(endField.getText, dateTimeFormatter)) match {
        case Success(dt) => event.endTime = dt
        case Failure(_) => endField.setText(event.endTime.format(dateTimeFormatter))
      }
    }
    endField.addFocusListener(new java.awt.event.FocusAdapter {
      override def focusLost(e: java.awt.event.FocusEvent): Unit = {
        Try(LocalDateTime.parse(endField.getText, dateTimeFormatter)) match {
          case Success(dt) => event.endTime = dt
          case Failure(_) => endField.setText(event.endTime.format(dateTimeFormatter))
        }
      }
    })

    gbc.gridx = 0
    gbc.gridy = 1
    gbc.gridwidth = 1
    gbc.weightx = 0
    detailsPanel.add(new JLabel("Start:"), gbc)
    gbc.gridx = 1
    detailsPanel.add(startField, gbc)
    gbc.gridx = 2
    detailsPanel.add(new JLabel("End:"), gbc)
    gbc.gridx = 3
    detailsPanel.add(endField, gbc)

    // Location field
    val locationField = new JTextField(event.location, 30)
    locationField.getDocument.addDocumentListener(new javax.swing.event.DocumentListener {
      def changedUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def removeUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def insertUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def update(): Unit = event.location = locationField.getText
    })

    gbc.gridx = 0
    gbc.gridy = 2
    gbc.gridwidth = 1
    gbc.weightx = 0
    detailsPanel.add(new JLabel("Location:"), gbc)
    gbc.gridx = 1
    gbc.gridwidth = 3
    gbc.weightx = 1.0
    detailsPanel.add(locationField, gbc)

    // Description field
    val descArea = new JTextArea(event.description, 2, 30)
    descArea.setLineWrap(true)
    descArea.setWrapStyleWord(true)
    descArea.getDocument.addDocumentListener(new javax.swing.event.DocumentListener {
      def changedUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def removeUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def insertUpdate(e: javax.swing.event.DocumentEvent): Unit = update()

      def update(): Unit = event.description = descArea.getText
    })
    val descScroll = new JScrollPane(descArea)
    descScroll.setPreferredSize(new Dimension(400, 50))

    gbc.gridx = 0
    gbc.gridy = 3
    gbc.gridwidth = 1
    gbc.weightx = 0
    detailsPanel.add(new JLabel("Description:"), gbc)
    gbc.gridx = 1
    gbc.gridwidth = 3
    gbc.weightx = 1.0
    gbc.fill = GridBagConstraints.BOTH
    detailsPanel.add(descScroll, gbc)

    // Right side: Actions
    val actionsPanel = new JPanel()
    actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS))
    actionsPanel.setOpaque(false)
    actionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0))

    // Calendar selection
    val calendarLabel = new JLabel("Calendar:")
    calendarLabel.setAlignmentX(Component.CENTER_ALIGNMENT)
    val calendarCombo = new JComboBox[String](calendars)
    calendarCombo.setSelectedItem(event.calendar)
    calendarCombo.setMaximumSize(new Dimension(120, 30))
    calendarCombo.addActionListener { _ =>
      event.calendar = calendarCombo.getSelectedItem.asInstanceOf[String]
    }

    actionsPanel.add(calendarLabel)
    actionsPanel.add(Box.createRigidArea(new Dimension(0, 5)))
    actionsPanel.add(calendarCombo)
    actionsPanel.add(Box.createRigidArea(new Dimension(0, 15)))

    // Add to Google button
    val addButton = new JButton("Add to Google")
    addButton.setAlignmentX(Component.CENTER_ALIGNMENT)
    addButton.setBackground(new Color(66, 133, 244))
    addButton.setForeground(Color.WHITE)
    addButton.setFocusPainted(false)
    addButton.setMaximumSize(new Dimension(120, 30))
    addButton.addActionListener { _ =>
      addToGoogleCalendar(event)
    }

    actionsPanel.add(addButton)
    actionsPanel.add(Box.createRigidArea(new Dimension(0, 10)))

    // Delete button
    val deleteButton = new JButton("Delete")
    deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT)
    deleteButton.setBackground(new Color(211, 47, 47))
    deleteButton.setForeground(Color.WHITE)
    deleteButton.setFocusPainted(false)
    deleteButton.setMaximumSize(new Dimension(120, 30))
    deleteButton.addActionListener { _ =>
      events -= event
      refreshEventsList()
    }

    actionsPanel.add(deleteButton)
    actionsPanel.add(Box.createVerticalGlue())

    panel.add(detailsPanel, BorderLayout.CENTER)
    panel.add(actionsPanel, BorderLayout.EAST)

    panel
  }

  private def addToGoogleCalendar(event: CalendarEvent): Unit = {
    // TODO Use the calender-selection
    try {
      val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
      val title = URLEncoder.encode(event.title, "UTF-8")
      val details = URLEncoder.encode(event.description, "UTF-8")
      val location = URLEncoder.encode(event.location, "UTF-8")
      val startDate = event.startTime.format(dateFormat)
      val endDate = event.endTime.format(dateFormat)

      val url = s"https://calendar.google.com/calendar/render?action=TEMPLATE" +
        s"&text=$title" +
        s"&dates=$startDate/$endDate" +
        s"&details=$details" +
        s"&location=$location" +
        s"&ctz=Europe/Berlin"

      val pb = new ProcessBuilder("firefox", url)
      pb.start()

      showInfo("Opening Google Calendar in Firefox...")
    } catch {
      case e: Exception =>
        showError(s"Error opening Google Calendar: ${e.getMessage}")
    }
  }

  private def showError(message: String): Unit = {
    SwingUtilities.invokeLater { () =>
      JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
    }
  }

  private def showInfo(message: String): Unit = {
    // TODO Use notifications
    //    SwingUtilities.invokeLater { () =>
    //      JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE)
    //    }
    println(s"Info: $message")
  }
}

object AddCalendarEvent {
  private[AddCalendarEvent] val logger = Logger[AddCalendarEvent]

  def main(args: Array[String]): Unit = {
    SwingUtilities.invokeLater { () =>
      val manager = new AddCalendarEvent()
      manager.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      manager.showApp()
    }
  }
}