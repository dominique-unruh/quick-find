package de.unruh.quickfind
package apps.calendar

import java.io.FileInputStream
import javax.mail.internet.MimeMessage
import scala.util.{Failure, Success}

object EAETest extends App {
  import javax.mail.Session
  import java.util.Properties
  import java.io.ByteArrayInputStream

  // Assuming you have a MimeMessage object
  val session = Session.getDefaultInstance(new Properties())
  val message = new MimeMessage(session, FileInputStream("/tmp/mail.eml"))

  // Extract email content
  val emailText = Email.extractTextFromMessage(message)
  val subject = Option(message.getSubject).getOrElse("")

  val event = LLM.extractAppointmentFromMessage(subject, emailText)
  println(event)
}

