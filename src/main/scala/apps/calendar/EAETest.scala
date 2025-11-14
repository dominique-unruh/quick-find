package de.unruh.quickfind
package apps.calendar

import java.io.FileInputStream
import javax.mail.internet.MimeMessage
import scala.util.{Failure, Success}

object EAETest extends App {
  import javax.mail.Session
  import java.util.Properties
  import java.io.ByteArrayInputStream

  val apiKey = "REMOVED-FROM-GIT"

  // Assuming you have a MimeMessage object
  val session = Session.getDefaultInstance(new Properties())
  val message = new MimeMessage(session, FileInputStream("/tmp/mail.eml"))

  EmailAppointmentExtractor.extractAppointment(message, apiKey) match {
     case Some(appointment) =>
       println(s"Found appointment: ${appointment.subject}")
       println(s"Date/Time: ${appointment.dateTime}")
       println(s"Location: ${appointment.location}")
       println(s"Summary: ${appointment.summary}")
       println(s"Participants: ${appointment.participants.mkString(", ")}")
     case None =>
       println("No appointment found in email")
  }
}

