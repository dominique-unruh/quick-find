package de.unruh.quickfind
package apps.calendar

import core.{DeferredVal, Utils}

import com.typesafe.scalalogging.Logger

import java.awt.dnd.{DnDConstants, DropTarget, DropTargetAdapter, DropTargetDragEvent, DropTargetDropEvent}
import java.awt.event.KeyEvent
import java.awt.{BorderLayout, Color, Component, Dimension, FlowLayout, Font, GridBagConstraints, GridBagLayout, Insets, KeyboardFocusManager}
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.{BorderFactory, Box, BoxLayout, JButton, JComboBox, JFrame, JLabel, JOptionPane, JPanel, JScrollPane, JTextArea, JTextField, ScrollPaneConstants, SwingUtilities, WindowConstants}
import scala.collection.mutable.ArrayBuffer
import scala.compiletime.uninitialized
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

// Main Application
class AddCalendarEvent extends JFrame {
  FixDndJava.installPatch()
  private given DeferredVal.CheckInitManager()
//  private val events = ArrayBuffer[CalendarEvent]()
  private val eventsPanel = DeferredVal[JPanel]
  private val scrollPane = DeferredVal[JScrollPane]

  initialize()

  private def initialize(): Unit = {
    setTitle("Add calendar events")
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    setSize(900, 600)
    setLayout(new BorderLayout())

    // Create toolbar
    val toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT))
    val newButton = new JButton("New Event")
    newButton.addActionListener { _ => addEventEditor() }

    toolbar.add(newButton)

    eventsPanel := new JPanel()
    eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS))
    eventsPanel.setBackground(Color.WHITE)

    scrollPane := new JScrollPane(eventsPanel)
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    scrollPane.getVerticalScrollBar.setUnitIncrement(16)

    // Set up drag and drop
    new DropTarget(scrollPane, dropTargetAdapter)

    add(toolbar, BorderLayout.NORTH)
    add(scrollPane, BorderLayout.CENTER)
    setLocationRelativeTo(null)
    setAlwaysOnTop(true)

    KeyboardFocusManager.getCurrentKeyboardFocusManager.addKeyEventDispatcher((event: KeyEvent) => {println(event.toString); event.getID match
      case KeyEvent.KEY_PRESSED | KeyEvent.KEY_RELEASED => event.getKeyCode match
//        case KeyEvent.VK_ESCAPE => close(); true
        case KeyEvent.VK_W if event.isControlDown => close(); true
        case KeyEvent.VK_Q if event.isControlDown => close(); true
        case _ => false
      case _ => false})
    
    DeferredVal.assertInitialized()
  }

  def close(): Unit = {
    setVisible(false)
    eventsPanel.removeAll()
  }
  
  def showApp(): Unit = {
    setVisible(true)
  }

/*  private def refreshEventsList(): Unit = {
    eventsPanel.removeAll()

    events.foreach { event =>
      val eventWidget = EventEditor(
        initialEvent = event,
        removeEvent = { event => { events -= event; refreshEventsList() } },
        showError = showError, showInfo = showInfo,
      )
      eventsPanel.add(eventWidget)
      eventsPanel.add(Box.createRigidArea(new Dimension(0, 10)))
    }

    eventsPanel.revalidate()
    eventsPanel.repaint()
  }*/

  private object dropTargetAdapter extends DropTargetAdapter {
    override def dragOver(dtde: DropTargetDragEvent): Unit = {
      dtde.acceptDrag(DnDConstants.ACTION_COPY)
    }

    override def drop(dtde: DropTargetDropEvent): Unit = {
      var success = false
      try {
        dtde.acceptDrop(DnDConstants.ACTION_COPY)
        val transferable = dtde.getTransferable

        transferable match {
          case FileTransferable(file) if Utils.firstLine(file).exists(_.startsWith("BEGIN:VCALENDAR")) =>
            addEventEditors(ICS.parseICS(file))
            success = true
          case FileTransferable(file) if file.getName.toLowerCase.endsWith(".eml") =>
            addEventEditors(Email.parseEmailFile(file))
            success = true
          case StringTransferable(content) if content.startsWith("BEGIN:VCALENDAR") =>
            addEventEditors(ICS.parseICS(content))
            success = true
          case StringTransferable(content) =>
            addEventEditors(Email.parseEmailContent(content))
            success = true
          case _ =>
            showError("Cannot process this drag and drop object")
            success = true
        }
      } catch {
        case NonFatal(e) =>
          e.printStackTrace()
          showError(s"Failed to parse dropped event: $e")
      } finally {
        dtde.dropComplete(success)
      }
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

  private def addEventEditors(events: IterableOnce[CalendarEvent]): Unit =
    for (event <- events) addEventEditor(event)

  private def addEventEditor(event: CalendarEvent = CalendarEvent()): Unit = {
    val eventWidget = EventEditor(initialEvent = event,
      removeEvent = { widget => eventsPanel.remove(widget); eventsPanel.revalidate(); eventsPanel.repaint() },
      showError = showError, showInfo = showInfo)
    eventsPanel.add(eventWidget)
    eventsPanel.revalidate()
    eventsPanel.repaint()
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