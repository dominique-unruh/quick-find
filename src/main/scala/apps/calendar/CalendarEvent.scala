package de.unruh.quickfind
package apps.calendar

import java.awt.{BorderLayout, Color, Component, Dimension, FlowLayout, Font, GridBagConstraints, GridBagLayout, Insets}
import javax.swing.*
import javax.swing.border.*
import java.awt.dnd.*
import java.awt.datatransfer.*
import java.io.{File, FileInputStream}
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZonedDateTime}
import scala.util.{Failure, Success, Try}
import scala.io.Source
import javax.mail.internet.MimeMessage
import javax.mail.{BodyPart, Multipart, Session}
import java.util.Properties
import scala.collection.mutable.ArrayBuffer
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.Logger

// Calendar Event Model
case class CalendarEvent(
                          title: String = "",
                          startTime: ZonedDateTime = ZonedDateTime.now().plusDays(1).withHour(10).withMinute(0),
                          endTime: Option[ZonedDateTime] = None,
                          description: String = "",
                          location: String =  "",
                          calendar: String = "private"
                        ) {
  def mapDescription(f: String => String): CalendarEvent =
    copy(description = f(description))
}


object CalendarEvent {
  val calendars: Seq[String] = Seq("private", "work", "family", "other")
}