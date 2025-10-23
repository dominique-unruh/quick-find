package de.unruh.quickfind
package apps.calendar

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage
import scala.io.Source
import scala.util.Try

object ICS {
  def parseICSFile(file: File): Seq[CalendarEvent] = {
    val content = Source.fromFile(file, "UTF-8").mkString
    parseICSContent(content)
  }

  def parseICSContent(content: String): Seq[CalendarEvent] = {
    // TODO This arrives here with additional newline in the calendar entries? Error in Mime-Extraction? Or part of ICS standard?
    val lines = content.split("\n").map(_.trim)
    var inEvent = false
    var currentEvent = Map[String, String]()
    val events = Seq.newBuilder[CalendarEvent]

    lines.foreach { line =>
      if (line.startsWith("BEGIN:VEVENT")) {
        inEvent = true
        currentEvent = Map[String, String]()
      } else if (line.startsWith("END:VEVENT") && inEvent) {
        inEvent = false
        events ++= createEventFromICS(currentEvent)
      } else if (inEvent && line.contains(":")) {
        val parts = line.split(":", 2)
        if (parts.length == 2) {
          val key = parts(0).split(";")(0)
          currentEvent = currentEvent + (key -> parts(1))
        }
      }
    }

    events.result()
  }

  private def createEventFromICS(data: Map[String, String]): Option[CalendarEvent] = {
    val title = data.getOrElse("SUMMARY", "Untitled Event")
    val start = parseICSDateTime(data.getOrElse("DTSTART", ""))
    val end = parseICSDateTime(data.getOrElse("DTEND", ""))
    val description = data.getOrElse("DESCRIPTION", "").replace("\\n", "\n")
    val location = data.getOrElse("LOCATION", "")

    if (start.isDefined && end.isDefined) {
      Some(CalendarEvent(title, start.get, end.get, description, location))
    } else
      None
  }

  private def parseICSDateTime(dateStr: String): Option[LocalDateTime] = {
    Try {
      if (dateStr.contains("T")) {
        val cleaned = dateStr.replace("Z", "").replace("-", "").replace(":", "")
        LocalDateTime.parse(cleaned, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
      } else {
        LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd")).withHour(9)
      }
    }.toOption
  }



}
