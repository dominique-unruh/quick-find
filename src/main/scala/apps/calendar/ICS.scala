package de.unruh.quickfind
package apps.calendar

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.{Calendar, Property}
import net.fortuna.ical4j.model.component.{VEvent, VTimeZone}
import net.fortuna.ical4j.model.property.{DateProperty, Description, Location, Summary, TzId}

import java.io.{File, FileInputStream, InputStream, Reader, StringReader}
import java.nio.charset.StandardCharsets
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage
import scala.io.Source
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.OptionConverters.RichOptional
import scala.util.{Try, Using}

object ICS {
  def fixTimeZone(time: Temporal, currentTimezone: Option[VTimeZone]): ZonedDateTime = time match {
    case time: ZonedDateTime =>
      if (time.getZone.getId.startsWith("ical4j"))
        time.toOffsetDateTime.toZonedDateTime
      else
        time
    case time: LocalDateTime =>
      currentTimezone match {
        case None => throw RuntimeException("In calendar event, no timezone specified")
        case Some(timezone) =>
          time.atZone(ZoneId.of(timezone.getTimeZoneId.getValue))
      }
    case _ =>
      throw RuntimeException(s"Unsure how to process $time (${time.getClass}")
  }

  def calendarToEvents(calendar: Calendar): Seq[CalendarEvent] = {
    val events = Seq.newBuilder[CalendarEvent]
    var currentTimezone: Option[VTimeZone] = None
    for (case component <- calendar.getComponentList.getAll.asScala.toSeq)
      component match {
        case timezone: VTimeZone =>
          currentTimezone = Some(timezone)
        case event: VEvent =>
          val title = event.getProperty[Summary](Property.SUMMARY).toScala.map(_.getValue).getOrElse("Untitled Event")
          val start = fixTimeZone(event.getDateTimeStart[Temporal].getDate, currentTimezone)
          val end = event.getEndDate[Temporal].toScala.map(d => fixTimeZone(d.getDate, currentTimezone))
          val description = event.getProperty[Description](Property.DESCRIPTION).toScala.map(_.getValue).getOrElse("")
          val location = event.getProperty[Location](Property.LOCATION).toScala.map(_.getValue).getOrElse("")
          events += CalendarEvent(title, start, end, description, location)
      }
    events.result()
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
