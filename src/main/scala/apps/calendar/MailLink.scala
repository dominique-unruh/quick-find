package de.unruh.quickfind
package apps.calendar

import de.unruh.quickfind.apps.calendar.MailLink.{MailInfo, getMailInfo, percentEncode}
import org.apache.commons.text.StringEscapeUtils
import org.apache.commons.text.StringEscapeUtils.escapeHtml4

import java.net.{URI, URL, URLEncoder}
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import java.util.Properties
import javax.mail.{Address, Session}
import javax.mail.internet.MimeMessage
import scala.collection.IterableOnce
import scala.collection.immutable.HashSet
import scala.jdk.CollectionConverters.*

class MailLink(whohasit: Option[String],
               host: URL = defaultHost,
               includeSubject: Boolean = true,
               includeDate: Boolean = true,
               includeFrom: Boolean = true,
               includeTo: Boolean = true) {

  def getLinkHtml(mailInfo: MailInfo): String = {
    val link = getLink(mailInfo)
    val text = mailInfo.from match {
      case Some(sender) => s"Email from ${escapeHtml4(sender.toString)}"
      case None => "Email"
    }
    s"""<a href="${escapeHtml4(link.toString)}">$text</a>"""
  }

  def getLinkHtml(mail: MimeMessage): String =
    getLinkHtml(getMailInfo(mail))

  def getLink(mailInfo: MailInfo): URL = {
    val fields = Iterator.newBuilder[(String, String)]
    if (includeSubject)
      for (subject <- mailInfo.subject)
        fields += "subject" -> subject
    if (includeDate)
      for (date <- mailInfo.date)
        fields += "date" -> date.format(RFC_1123_DATE_TIME)
    if (includeFrom)
      for (from <- mailInfo.from)
        fields += "from" -> from.toString
    if (includeTo && mailInfo.to.nonEmpty)
      fields += "to" -> mailInfo.to.map(_.toString).mkString(", ")

    val url: StringBuilder = StringBuilder()
    url ++= host.toString
    url += '#'
    url ++= percentEncode(mailInfo.messageId)
    for ((key, value) <- fields.result()) {
      url += '&'
      url ++= key
      url += '='
      url ++= percentEncode(value)
    }

    val lastChar = url.last
    if (!MailLink.allowedEndChar(lastChar)) {
      url.length = url.length - 1
      url ++= "%%%02X".format(lastChar.toInt)
    }

    URI(url.result()).toURL
  }

  def getLink(mail: MimeMessage): URL =
    getLink(getMailInfo(mail))
}

object MailLink {
  case class MailInfo(messageId: String,
                      subject: Option[String],
                      date: Option[ZonedDateTime],
                      from: Option[Address],
                      to: Seq[Address])
  val defaultHost: URL = new URI("https://qis.rwth-aachen.de/people/unruh/tools/mail-link/").toURL

  def getMailInfo(mail: MimeMessage): MailInfo = {
    val messageId = mail.getMessageID.stripPrefix("<").stripSuffix(">")
    val subject = Option(mail.getSubject)
    val date = Option(mail.getHeader("Date", null))
      .map(raw => ZonedDateTime.parse(raw.trim, RFC_1123_DATE_TIME))
    val to = Option(mail.getAllRecipients).map(_.toSeq).getOrElse(Seq.empty)
    val from = Option(mail.getSender)
    MailInfo(messageId=messageId, to=to, from=from, date=date, subject=subject)
  }

  private val dontEncode = {
    val set = HashSet.newBuilder[Char]
    for (c <- 'a' to 'z')
      set += c
    for (c <- 'A' to 'Z')
      set += c
    for (c <- '0' to '9')
      set += c
    set ++= "-_.~@"
    set.result()
  }
  private val allowedEndChar = {
    val set = HashSet.newBuilder[Char]
    for (c <- 'a' to 'z')
      set += c
    for (c <- 'A' to 'Z')
      set += c
    for (c <- '0' to '9')
      set += c
    set ++= "/&#="
    set.result()
  }
  private def percentEncode(string: String) = {
    val bytes = string.getBytes(UTF_8)
    val sb = new StringBuilder
    for (b <- bytes) {
      val c = (b & 0xFF).toChar
      if (dontEncode(c))
        sb.append(c)
      else if (c == ' ')
        sb.append('+')
      else
        sb.append("%%%02X".format(c.toInt))
    }
    sb.toString
  }

  def main(args: Array[String]): Unit = {
    val emailFile = Path.of("/home/unruh/r/home/reisen/old/2010/2010-08 Crypto Santa Barbara/flug.eml")
    val mail = new MimeMessage(Session.getDefaultInstance(new Properties()), Files.newInputStream(emailFile))
    println(MailLink(whohasit = Some("Dominique Unruh")).getLink(mail))
  }
}