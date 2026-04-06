package de.unruh.quickfind
package apps.calendar

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.api.services.calendar.model.{Event, EventDateTime}
import de.unruh.quickfind.core.Persistence

import java.io.{File, InputStreamReader}
import java.nio.file.{Files, Path}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.{Collections, Date}
import scala.jdk.CollectionConverters.*

object GoogleCalendarClient {
  private lazy val service = GoogleCalendarClient.buildService()

  private val APPLICATION_NAME = "quickfind - add calendar entry"
  private val JSON_FACTORY     = GsonFactory.getDefaultInstance
  private val TOKENS_DIR       = Persistence.quickfindDir.resolve("GoogleCalendarClient-tokens")
  private val SCOPES           = Collections.singletonList(CalendarScopes.CALENDAR)

  // ── Auth ──────────────────────────────────────────────────────────────────

  private def authorize(): Credential = {
    val secretsPath  = Path.of(".google-credentials.json")
    val secrets = GoogleClientSecrets.load(JSON_FACTORY, Files.newBufferedReader(secretsPath))

    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val flow = new GoogleAuthorizationCodeFlow.Builder(
      httpTransport, JSON_FACTORY, secrets, SCOPES
    ).setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIR.toFile))
      .setAccessType("offline")
      .build()

    val receiver = new LocalServerReceiver.Builder().setPort(8888).build()
    new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
  }

  def buildService(): Calendar = {
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    new Calendar.Builder(httpTransport, JSON_FACTORY, authorize())
      .setApplicationName(APPLICATION_NAME)
      .build()
  }

  // ── List calendars ────────────────────────────────────────────────────────

  /** Returns a map of calendar display name → calendar ID. */
  def listCalendars(service: Calendar): Map[String, String] =
    service.calendarList().list().execute()
      .getItems.asScala
      .map(c => c.getSummary -> c.getId)
      .toMap

  // ── Create event ──────────────────────────────────────────────────────────

  def createEvent(event: CalendarEvent, showError: String => Unit): Unit = {
    try {
      val calendarMap = listCalendars(service)

      val calendarId = calendarMap.getOrElse(
        event.calendar,
        throw new IllegalArgumentException(
          s"Calendar '${event.calendar}' not found. Available: ${calendarMap.keys.mkString(", ")}"
        )
      )

      val resolvedEnd = event.endTime.getOrElse(event.startTime.plusHours(1))

      def toEventDateTime(zdt: ZonedDateTime): EventDateTime =
        new EventDateTime()
          .setDateTime(new DateTime(Date.from(zdt.toInstant)))
          .setTimeZone(zdt.getZone.getId)

      val gEvent = new Event()
        .setSummary(event.title)
        .setLocation(event.location)
        .setDescription(event.description) // HTML passed as-is
        .setStart(toEventDateTime(event.startTime))
        .setEnd(toEventDateTime(resolvedEnd))

      service.events().insert(calendarId, gEvent).execute()

      val time = event.startTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
      val url = s"https://calendar.google.com/calendar/u/0/r/week/$time"
      new ProcessBuilder("firefox", url).start()
    } catch
    {
      case e: Exception =>
        e.printStackTrace()
        showError(s"Error opening Google Calendar: ${e.getMessage}")
    }
  }

  val calendars: Map[String, String] = Map(
    "private" -> "private",
    "work" -> "Dominique Unruh",
  )
}