package de.unruh.quickfind
package apps.calendar

import com.google.api.client.auth.oauth2.{Credential, TokenResponseException}
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.api.services.calendar.model.{Event, EventDateTime}
import com.typesafe.scalalogging.Logger
import de.unruh.quickfind.core.Cache

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
  private val TOKENS_DIR       = Cache.quickfindDir.resolve("GoogleCalendarClient-tokens")
  private val SCOPES           = Collections.singletonList(CalendarScopes.CALENDAR)

  // ── Auth ──────────────────────────────────────────────────────────────────

  private def authorize(forceNew: Boolean = false): Credential = {
    val secretsPath  = Path.of(".google-credentials.json")
    val secrets = GoogleClientSecrets.load(JSON_FACTORY, Files.newBufferedReader(secretsPath))

    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val flow = new GoogleAuthorizationCodeFlow.Builder(
      httpTransport, JSON_FACTORY, secrets, SCOPES
    ).setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIR.toFile))
      .setAccessType("offline")
      .build()

    if (forceNew)
      flow.getCredentialDataStore.delete("user")

    val receiver = new LocalServerReceiver.Builder().setPort(8888).build()
    val credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    credential
  }

  def buildService(reauthorize: Boolean = false): Calendar = {
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val service = new Calendar.Builder(httpTransport, JSON_FACTORY, authorize(reauthorize))
      .setApplicationName(APPLICATION_NAME)
      .build()

    // reauthorize if needed
    if (!reauthorize) try {
      service.calendarList().list().setMaxResults(1).execute()
    } catch {
      case e: TokenResponseException
        if Option(e.getDetails).exists(_.getError == "invalid_grant") =>
      return buildService(reauthorize = true)
    }

    service
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

  private val logger = Logger[GoogleCalendarClient.type]
}