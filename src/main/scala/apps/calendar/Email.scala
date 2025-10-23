package de.unruh.quickfind
package apps.calendar

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.util.Properties
import javax.mail.{BodyPart, Multipart, Session}
import javax.mail.internet.MimeMessage
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object Email {
  def strippedSubject(message: MimeMessage): String = {
    var subject = message.getSubject
    var prevSubject = ""
    while (subject != prevSubject) {
      prevSubject = subject
      subject = subject.stripPrefix("Re:").stripPrefix("AW:").stripPrefix("WG:").stripLeading()
    }
    subject
  }

  def getEmailBody(message: MimeMessage): String = {
    val content = message.getContent
    content match {
      case text: String => text
      case multipart: Multipart =>
        val sb = new StringBuilder
        for (i <- 0 until multipart.getCount) {
          val bodyPart = multipart.getBodyPart(i)
          if (bodyPart.isMimeType("text/plain")) {
            sb.append(bodyPart.getContent.toString)
          }
        }
        sb.toString
      case _ => ""
    }
  }

  private def extractICSAttachments(message: MimeMessage): List[String] = {
    val attachments = ArrayBuffer[String]()

    def processBodyPart(bodyPart: BodyPart): Unit = {
      val disposition = bodyPart.getDisposition
      val hasIcsSuffix = Option(bodyPart.getFileName).getOrElse("").toLowerCase.endsWith(".ics")
      val hasCalendarMimeType = bodyPart.isMimeType("text/calendar")
      val isCalendarEntry = hasIcsSuffix || hasCalendarMimeType

      if (isCalendarEntry) {
        val content = Source.fromInputStream(bodyPart.getInputStream).mkString
        attachments += content
      }
    }

    val content = message.getContent
    content match {
      case multipart: Multipart =>
        for (i <- 0 until multipart.getCount) {
          processBodyPart(multipart.getBodyPart(i))
        }
      case _ =>
    }

    attachments.toList
  }

  def parseEmailContent(content: String): Seq[CalendarEvent] = {
    val session = Session.getDefaultInstance(new Properties())
    val is = new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
    try {
      val message = new MimeMessage(session, is)
      processEmail(message)
    } finally
      is.close()
  }

  def parseEmailFile(file: File): Seq[CalendarEvent] = {
    val session = Session.getDefaultInstance(new Properties())
    val fis = new FileInputStream(file)
    val message = new MimeMessage(session, fis)
    val events = processEmail(message)
    fis.close()
    events
  }

  private def processEmail(message: MimeMessage): Seq[CalendarEvent] = {
    val events = Seq.newBuilder[CalendarEvent]
    val icsAttachments = Email.extractICSAttachments(message)
    val messageId = message.getMessageID.stripPrefix("<").stripSuffix(">")

    val descriptionPrefix = s"$messageId\n\n${Email.getEmailBody(message)}\n\n"

    if (icsAttachments.nonEmpty) {
      for (attachment <- icsAttachments;
           event <- ICS.parseICSContent(attachment))
        events += event.prefixDescriptionWith(descriptionPrefix)
    } else {
      val subject = Email.strippedSubject(message)
      val body = Email.getEmailBody(message)
      events += CalendarEvent(title = subject, description = descriptionPrefix)
      //      extractEventWithLLM(subject, subject, body)
    }
    events.result()
  }

}
