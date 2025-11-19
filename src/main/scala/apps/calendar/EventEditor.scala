package de.unruh.quickfind
package apps.calendar

import com.typesafe.scalalogging.Logger
import de.unruh.quickfind.apps.calendar.EventEditor.logger
import de.unruh.quickfind.apps.nextcloud.UploadingTransferHandler

import java.awt.{BorderLayout, Color, Component, Desktop, Dimension, Font, GridBagConstraints, GridBagLayout, Insets}
import java.net.{URI, URLEncoder}
import java.time.{LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter
import javax.swing.{BorderFactory, Box, BoxLayout, JButton, JComboBox, JLabel, JPanel, JScrollPane, JTextArea, JTextField}
import scala.compiletime.uninitialized
import scala.util.control.NonFatal

class EventEditor(event: CalendarEvent,
                  showError: String => Unit,
                  showInfo: String => Unit,
                  removeEvent: CalendarEvent => Unit) extends JPanel {
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
  private var titleField: JTextField = uninitialized
  private var startField: JTextField = uninitialized
  private var endField: JTextField = uninitialized
  private var locationField: JTextField = uninitialized
  private var descArea: JTextArea = uninitialized
  private var calendarCombo: JComboBox[String] = uninitialized
  private var eventIsValid: Boolean = false

  initialize()

  private def getEvent(): Option[CalendarEvent] = {
    var success = true
    val title = titleField.getText
    val location = locationField.getText
    val description = descArea.getText
    var startTime: Option[ZonedDateTime] = None
    var endTime: Option[ZonedDateTime] = None
    try
      startTime = Some(ZonedDateTime.parse(startField.getText, dateTimeFormatter))
      warnColor(startField, false)
    catch
      case NonFatal(e) =>
        logger.debug(s"Parsing start time: ${e.toString}")
        success = false
        warnColor(startField, true)

    if (endField.getText.nonEmpty)
      warnColor(endField, false)
    else
      try
        endTime = Some(ZonedDateTime.parse(endField.getText, dateTimeFormatter))
        warnColor(endField, false)
      catch
        case NonFatal(e) =>
          logger.debug(s"Parsing end time: ${e.toString}")
          success = false
          warnColor(endField, true)

    eventIsValid = success

    if (success)
      Some(CalendarEvent(title = title, location = location, startTime = startTime.get, endTime = endTime, description = description,
        calendar = calendarCombo.getSelectedItem.asInstanceOf[String]))
    else
      None
  }

  private def warnColor(component: JTextField, warn: Boolean): Unit = {
    if (warn)
      component.setBackground(Color.RED)
    else
      component.setBackground(Color.WHITE)
  }

  def initialize(): Unit = {
    val updateListener = UniversalChangeListener(() => getEvent())
    setLayout(new BorderLayout(10, 10))
    setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(Color(200, 200, 200), 1),
      BorderFactory.createEmptyBorder(15, 15, 15, 15)
    ))
    setBackground(new Color(250, 250, 250))
    setMaximumSize(new Dimension(Integer.MAX_VALUE, 200))

    // Left side: Event details
    val detailsPanel = new JPanel(new GridBagLayout())
    detailsPanel.setOpaque(false)
    val gbc = new GridBagConstraints()
    gbc.insets = new Insets(3, 5, 3, 5)
    gbc.anchor = GridBagConstraints.WEST
    gbc.fill = GridBagConstraints.HORIZONTAL

    // Title field (larger)
    titleField = new JTextField(event.title, 30)
    titleField.setFont(titleField.getFont.deriveFont(Font.BOLD, 14f))
    titleField.getDocument.addDocumentListener(updateListener)

    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    detailsPanel.add(new JLabel("Title:"), gbc)
    gbc.gridx = 1
    gbc.gridwidth = 3
    gbc.weightx = 1.0
    detailsPanel.add(titleField, gbc)

    // Date/Time fields
    startField = new JTextField(event.startTime.format(dateTimeFormatter), 15)
    startField.addActionListener(updateListener)
    startField.addFocusListener(updateListener)

    endField = new JTextField(event.endTime.map(_.format(dateTimeFormatter))
      .getOrElse(event.startTime.plusHours(1).format(dateTimeFormatter)), 15)
    endField.addActionListener(updateListener)
    endField.addFocusListener(updateListener)

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
    locationField = new JTextField(event.location, 30)
    locationField.getDocument.addDocumentListener(updateListener)

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
    descArea = new JTextArea(event.description, 2, 30)
    descArea.setLineWrap(true)
    descArea.setWrapStyleWord(true)
    descArea.getDocument.addDocumentListener(updateListener)
    descArea.setTransferHandler(UploadingTransferHandler(
      errorMessage = showError,
      infoMessage = showInfo,
    ))
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
    calendarCombo = new JComboBox[String](CalendarEvent.calendars.toArray)
    calendarCombo.setSelectedItem(event.calendar)
    calendarCombo.setMaximumSize(new Dimension(120, 30))
    calendarCombo.addActionListener(updateListener)

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
    deleteButton.addActionListener(UniversalChangeListener(() => removeEvent(event)))

    actionsPanel.add(deleteButton)
    actionsPanel.add(Box.createVerticalGlue())

    add(detailsPanel, BorderLayout.CENTER)
    add(actionsPanel, BorderLayout.EAST)

    getEvent() // Checks fields
  }

/*  private def addToGoogleCalendar(event: CalendarEvent): Unit = {
    if (!eventIsValid)
      showError("Cannot add the event. Not currently valid")
      return
    // TODO Use the calender-selection
    try {
      val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
      val title = URLEncoder.encode(event.title, "UTF-8")
      val details = URLEncoder.encode(event.description, "UTF-8")
      val location = URLEncoder.encode(event.location, "UTF-8")
      val startDate = event.startTime.format(dateFormat)
      val endDate = event.endTime.map(_.format(dateFormat))
      val startEndDate = endDate match
        case None => startDate
        case Some(end) => s"$startDate/$end"

      val url = s"https://calendar.google.com/calendar/render?action=TEMPLATE" +
        s"&text=$title" +
        s"&dates=$startEndDate" +
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
  }*/

  val calendars: Map[String, String] = Map(
    "private" -> "private",
    "work" -> "Dominique Unruh",
  )

  def addToGoogleCalendar(calendarEvent: CalendarEvent): Unit = {
    // Get the calendar ID from the map
    val calendarId = calendars.getOrElse(
      calendarEvent.calendar,
      throw RuntimeException(s"Calendar '${calendarEvent.calendar}' not found in configured calendars")
    )

    // Format times as yyyyMMdd'T'HHmmss'Z' in UTC or yyyyMMdd'T'HHmmss for local time
    val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    val startTimeFormatted = calendarEvent.startTime.format(dateFormatter)
    val endTimeFormatted = calendarEvent.endTime
      .getOrElse(calendarEvent.startTime.plusHours(1))
      .format(dateFormatter)

    // Build the Google Calendar URL
    val params = Seq(
      "action" -> "TEMPLATE",
      "text" -> calendarEvent.title,
      "dates" -> s"$startTimeFormatted/$endTimeFormatted",
      "details" -> calendarEvent.description,
      "location" -> calendarEvent.location,
      "src" -> calendarId
    ).filter(_._2.nonEmpty) // Remove empty parameters
      .map { case (key, value) =>
        s"$key=${URLEncoder.encode(value, "UTF-8")}"
      }
      .mkString("&")

    val url = s"https://calendar.google.com/calendar/render?$params"

    // Open in default browser
//    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)) {
    Desktop.getDesktop.browse(new URI(url))
  }
}

object EventEditor {
  private val logger = Logger[EventEditor]
}
