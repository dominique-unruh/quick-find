package de.unruh.quickfind
package apps.calendar

import items.DirectItem
import core.{Item, ScalableImage}

import java.nio.file.Path

class CalendarEventItem(val parent: Item) extends DirectItem {
  override val underlyingFile: Option[Path] = None

  private lazy val app = new AddCalendarEvent()
  override val persistentKey: Array[Byte] = Array.empty

  override def title: String = "Add calendar event"

  override def defaultAction(): Unit =
    app.showApp()

  override def previewLine: String = "Add calendar entries by drag and drop"

  // TODO
  override def icon: ScalableImage = Item.defaultIcon
}
