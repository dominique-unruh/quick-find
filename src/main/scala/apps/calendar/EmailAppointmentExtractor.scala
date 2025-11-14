package de.unruh.quickfind.apps.calendar

import de.unruh.quickfind.core.Persistence

import javax.mail.internet.MimeMessage
import javax.mail.{BodyPart, Multipart, Part}
import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import play.api.libs.json.*

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import scala.util.control.NonFatal

// Costs get aggregated here: https://portal.withorb.com/view?token=Im55RE5ZZlI5R2pFcmMyYlEi.0C2J4nSd2tgGrS0uqXPkySWR9EY

case class AppointmentInfo(
                            dateTime: Option[ZonedDateTime],
                            location: Option[String],
                            subject: String,
                            summary: String,
                            participants: List[String]
                          )

object EmailAppointmentExtractor {

  private val PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions"
  private val CET_ZONE = ZoneId.of("CET")

  /**
   * Extracts appointment information from a MimeMessage using Perplexity AI
   * @param message The parsed email message
   * @param apiKey Your Perplexity API key
   * @return Try[Option[AppointmentInfo]] - Success with Some if appointment found, None if not found
   */
  def extractAppointment(message: MimeMessage, apiKey: String): Option[AppointmentInfo] = {
    // Extract email content
    val emailText = extractTextFromMessage(message)
    val subject = Option(message.getSubject).getOrElse("")

    // Call Perplexity API
    val prompt = buildPrompt(emailText, subject)
    val response = callPerplexityAPI(prompt, apiKey)

    // Parse the response
    parseAppointmentResponse(response)
  }

  private def extractTextFromMessage(message: MimeMessage): String = {
    val content = message.getContent
    content match {
      case text: String => text
      case multipart: Multipart =>
        (0 until multipart.getCount)
          .map(multipart.getBodyPart)
          .flatMap(extractTextFromPart)
          .mkString("\n")
      case _ => ""
    }
  }

  private def extractTextFromPart(part: BodyPart): Option[String] = {
    if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
      Try(part.getContent.toString).toOption
    } else if (part.isMimeType("multipart/*")) {
      part.getContent match {
        case multipart: Multipart =>
          Some((0 until multipart.getCount)
            .map(multipart.getBodyPart)
            .flatMap(extractTextFromPart)
            .mkString("\n"))
        case _ => None
      }
    } else {
      None
    }
  }

  private def buildPrompt(emailContent: String, emailSubject: String): String = {
    s"""Analyze the following email and extract appointment information if present.
       |
       |Email Subject: $emailSubject
       |Email Content:
       |$emailContent
       |
       |Please extract the following information if an appointment or meeting is mentioned:
       |1. Date and time (assume CET timezone if not specified, provide in ISO-8601 format with timezone)
       |2. Location (physical address or virtual meeting link)
       |3. Event subject/title
       |4. Brief summary (max 100 words)
       |5. Names of people involved
       |
       |Respond ONLY with a JSON object in this exact format:
       |{
       |  "hasAppointment": true/false,
       |  "dateTime": "ISO-8601 datetime string with timezone or null",
       |  "location": "location string or null",
       |  "subject": "subject string",
       |  "summary": "summary text max 100 words",
       |  "participants": ["name1", "name2"]
       |}
       |
       |If no appointment is found, set hasAppointment to false and set other fields to null or empty.
       |""".stripMargin
  }

  private def callPerplexityAPI(prompt: String, apiKey: String): String = {
    // Use https://docs.perplexity.ai/guides/structured-outputs ?
    val requestBody = Json.obj(
      "model" -> "sonar",  // Using the current sonar model for fast, straightforward answers
      "messages" -> Json.arr(
        Json.obj(
          "role" -> "system",
          "content" -> "You are a helpful assistant that extracts appointment information from emails. Always respond with valid JSON only."
        ),
        Json.obj(
          "role" -> "user",
          "content" -> prompt
        )
      ),
      "temperature" -> 0.2,
      "max_tokens" -> 1000,
      "disable_search" -> true
    )

    val requestBodyStr = requestBody.toString()

    cachedQuery(requestBodyStr, apiKey)
  }

  private val table = "perplexity-chat2".getBytes

  private def cachedQuery(requestBody: String, apiKey: String): String = synchronized {
    val key = requestBody.getBytes(UTF_8)
    Persistence.get(table, getClass, key) match {
      case Some(value) => String(value, UTF_8)
      case None =>
        println("Doing query.")
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
          .uri(URI.create(PERPLEXITY_API_URL))
          .header("Authorization", s"Bearer $apiKey")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
          throw new RuntimeException(s"Perplexity API error: ${response.statusCode()} - ${response.body()}")
        }

        val responseString = response.body()
        Persistence.put(table, getClass, key, responseString.getBytes(UTF_8))
        responseString
      }
  }

  private def parseAppointmentResponse(jsonResponse: String): Option[AppointmentInfo] = {
    println(jsonResponse)
    val json = Json.parse(jsonResponse)
    val contentText = (json \ "choices" \ 0 \ "message" \ "content").as[String]

    // Extract JSON from the content (handle potential markdown code blocks)
    val jsonContent = if (contentText.trim.startsWith("```")) {
      contentText.split("```").find(_.trim.startsWith("{")).getOrElse(contentText)
    } else {
      contentText
    }
//    println(contentText.trim)

    val appointmentJson = Json.parse(jsonContent.trim)

    val hasAppointment = (appointmentJson \ "hasAppointment").asOpt[Boolean].getOrElse(false)

    if (!hasAppointment) {
      None
    } else {
      val dateTimeStr = (appointmentJson \ "dateTime").asOpt[String]
      val dateTime = dateTimeStr.map { dt =>
        try ZonedDateTime.parse(dt, DateTimeFormatter.ISO_DATE_TIME)
        catch
          case NonFatal(_) => ZonedDateTime.parse(dt)
      }

      Some(AppointmentInfo(
        dateTime = dateTime,
        location = (appointmentJson \ "location").asOpt[String].filter(_.nonEmpty),
        subject = (appointmentJson \ "subject").asOpt[String].getOrElse("Untitled Event"),
        summary = (appointmentJson \ "summary").asOpt[String].getOrElse(""),
        participants = (appointmentJson \ "participants").asOpt[List[String]].getOrElse(List.empty)
      ))
    }
  }
}

// Example usage:
/*
object Main extends App {
  import javax.mail.Session
  import java.util.Properties
  import java.io.ByteArrayInputStream

  val apiKey = "your-perplexity-api-key"

  // Assuming you have a MimeMessage object
  val session = Session.getDefaultInstance(new Properties())
  // val message = new MimeMessage(session, inputStream)

  // EmailAppointmentExtractor.extractAppointment(message, apiKey) match {
  //   case Success(Some(appointment)) =>
  //     println(s"Found appointment: ${appointment.subject}")
  //     println(s"Date/Time: ${appointment.dateTime}")
  //     println(s"Location: ${appointment.location}")
  //     println(s"Summary: ${appointment.summary}")
  //     println(s"Participants: ${appointment.participants.mkString(", ")}")
  //   case Success(None) =>
  //     println("No appointment found in email")
  //   case Failure(exception) =>
  //     println(s"Error: ${exception.getMessage}")
  // }
}
*/