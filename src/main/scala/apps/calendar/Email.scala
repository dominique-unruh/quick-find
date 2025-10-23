package de.unruh.quickfind
package apps.calendar

import javax.mail.Multipart
import javax.mail.internet.MimeMessage

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
}
