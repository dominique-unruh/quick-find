package de.unruh.quickfind
package apps.calendar

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.{Calendar, Property}
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.{Description, Location, Summary}

import java.io.{File, FileInputStream, InputStream, Reader, StringReader}
import java.nio.charset.StandardCharsets
import java.time.{LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage
import scala.io.Source
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.OptionConverters.RichOptional
import scala.util.{Try, Using}

object ICS {
  def fixTimeZone(time: ZonedDateTime): ZonedDateTime = {
    if (time.getZone.getId.startsWith("ical4j~"))
      time.toOffsetDateTime.toZonedDateTime
    else
      time
  }

  def calendarToEvents(calendar: Calendar): Seq[CalendarEvent] =
    for (case event: VEvent <- calendar.getComponentList.getAll.asScala.toSeq)
      yield {
        val title = event.getProperty[Summary](Property.SUMMARY).toScala.map(_.getValue).getOrElse("Untitled Event")
        val start = fixTimeZone(event.getDateTimeStart[ZonedDateTime].getDate)
        val end = event.getEndDate[ZonedDateTime].toScala.map(d => fixTimeZone(d.getDate))
        val description = event.getProperty[Description](Property.DESCRIPTION).toScala.map(_.getValue).getOrElse("")
        val location = event.getProperty[Location](Property.LOCATION).toScala.map(_.getValue).getOrElse("")

        CalendarEvent(title, start, end, description, location)
      }

  def parseICS(istream: InputStream): Seq[CalendarEvent] =
    calendarToEvents(CalendarBuilder().build(istream))
  def parseICS(reader: Reader): Seq[CalendarEvent] =
    calendarToEvents(CalendarBuilder().build(reader))
  def parseICS(file: File): Seq[CalendarEvent] =
    parseICS(FileInputStream(file))
  def parseICS(content: String): Seq[CalendarEvent] =
    parseICS(StringReader(content))

  private def createEventFromICS(data: Map[String, String]): Option[CalendarEvent] = {
    val title = data.getOrElse("SUMMARY", "Untitled Event")
    val start = parseICSDateTime(data.getOrElse("DTSTART", ""))
    val end = data.get("DTEND").map(parseICSDateTime)
    val description = data.getOrElse("DESCRIPTION", "").replace("\\n", "\n")
    val location = data.getOrElse("LOCATION", "")

    Some(CalendarEvent(title, start, end, description, location))
  }

  private def parseICSDateTime(dateStr: String): ZonedDateTime = {
    if (dateStr.contains("T")) {
      val cleaned = dateStr.replace("Z", "").replace("-", "").replace(":", "")
      ZonedDateTime.parse(cleaned, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
    } else {
      ZonedDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd")).withHour(9)
    }
  }



}
