package de.unruh.quickfind.apps.calendar

import com.typesafe.scalalogging.Logger
import de.unruh.quickfind.core.Persistence

import javax.mail.internet.MimeMessage
import javax.mail.{BodyPart, Multipart, Part}
import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try, Using}
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import play.api.libs.json.*

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source
import scala.util.control.NonFatal

// Costs get aggregated here: https://portal.withorb.com/view?token=Im55RE5ZZlI5R2pFcmMyYlEi.0C2J4nSd2tgGrS0uqXPkySWR9EY

/*case class AppointmentInfo(
                            dateTime: Option[ZonedDateTime],
                            location: Option[String],
                            subject: String,
                            summary: String,
                            participants: List[String]
                          )*/

object LLM {
  private val PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions"
  private val CET_ZONE = ZoneId.of("CET")
  private lazy val API_KEY = Using.resource(Source.fromFile("/home/unruh/.config/unruh/perplexity-api-key-extract-calendar-items"))(_.mkString)

  /**
   * Extracts appointment information from a MimeMessage using Perplexity AI
   * @param message The parsed email message
   * @return Success with Some if appointment found, None if not found
   */
  def extractAppointmentFromMessage(subject: String, text: String): CalendarEvent = {
    // Call Perplexity API
    val prompt = buildPromptMessage(text, subject)
    logger.debug(s"Prompt:\n$prompt\n")
    val response = callPerplexityAPI(prompt)

    // Parse the response
    parseAppointmentResponse(response)
  }

  private def buildPromptMessage(emailContent: String, emailSubject: String): String = {
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
       |  "summary": "summary text max 100 words (HTML)",
       |  "participants": ["name1", "name2"]
       |}
       |
       |If no appointment is found, set hasAppointment to false and set other fields to null or empty.
       |""".stripMargin
  }

  private def callPerplexityAPI(prompt: String): String = {
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

    cachedQuery(requestBodyStr)
  }

  private val table = "perplexity-chat2".getBytes

  private def cachedQuery(requestBody: String): String = synchronized {
    val key = requestBody.getBytes(UTF_8)
    Persistence.get(table, getClass, key) match {
      case Some(value) => String(value, UTF_8)
      case None =>
        println("Doing query.")
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
          .uri(URI.create(PERPLEXITY_API_URL))
          .header("Authorization", s"Bearer $API_KEY")
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

  private def parseAppointmentResponse(jsonResponse: String): CalendarEvent = {
    val json = Json.parse(jsonResponse)
    val contentText = (json \ "choices" \ 0 \ "message" \ "content").as[String]

    // Extract JSON from the content (handle potential markdown code blocks)
    val jsonContent = if (contentText.trim.startsWith("```")) {
      contentText.split("```(json)?").find(_.trim.startsWith("{")).getOrElse(contentText)
    } else {
      contentText
    }
    logger.debug(s"Response: ${contentText.trim}")

    val appointmentJson = Json.parse(jsonContent.trim)

    val hasAppointment = (appointmentJson \ "hasAppointment").asOpt[Boolean].getOrElse(false)

    if (!hasAppointment)
      throw RuntimeException("No appointment found")

    val dateTimeStr = (appointmentJson \ "dateTime").as[String]
    val dateTime =
      try ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME)
      catch
        case NonFatal(_) => ZonedDateTime.parse(dateTimeStr)

    val title = (appointmentJson \ "subject").as[String]
    val summary = (appointmentJson \ "summary").asOpt[String].getOrElse("")
    val participants = (appointmentJson \ "participants").asOpt[List[String]].getOrElse(List.empty)
    val location = (appointmentJson \ "location").asOpt[String].getOrElse("")
    val titleWithParticipants = participants match
      case Nil => title
      case participants => s"$title / ${participants.mkString(", ")}"

    CalendarEvent(
      title = title,
      startTime = dateTime,
      endTime = None,
      description = summary,
      location = location,
      calendar = "work",
    )
  }

  private val logger = Logger[LLM.type]
}
