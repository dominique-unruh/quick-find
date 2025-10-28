package de.unruh.quickfind
package apps.calendar

import java.awt.event.{ActionEvent, ActionListener, FocusEvent, FocusListener}
import javax.swing.event.{DocumentEvent, DocumentListener}

class UniversalChangeListener(callback: () => Unit) extends DocumentListener, ActionListener, FocusListener {
  override def insertUpdate(documentEvent: DocumentEvent): Unit = callback()
  override def removeUpdate(documentEvent: DocumentEvent): Unit = callback()
  override def changedUpdate(documentEvent: DocumentEvent): Unit = callback()
  override def actionPerformed(actionEvent: ActionEvent): Unit = callback()
  override def focusGained(focusEvent: FocusEvent): Unit = callback()
  override def focusLost(focusEvent: FocusEvent): Unit = callback()
}
