package de.unruh.quickfind
package apps.nextcloud

import com.typesafe.scalalogging.Logger
import org.aarboard.nextcloud.api.NextcloudConnector
import org.aarboard.nextcloud.api.filesharing.{SharePermissions, ShareType}
import org.apache.commons.io.FilenameUtils

import scala.sys.process.given

import java.io.File
import scala.io.Source
import scala.util.{Random, Using}

object Nextcloud {
  private val nextcloudFolder = "/home/unruh/cloud/sciebo/tmp/mail-attachments/"
  private val nextcloudUrl = "https://rwth-aachen.sciebo.de"
  private val nextcloudUser = "5QCB65@rwth-aachen.de"
  private val nextcloudPassword = "secret-tool lookup username 5QCB65@rwth-aachen.de service sciebo".!!.trim
  private val nextcloudRemotePath = "/tmp/mail-attachments/" // Remote path in NextCloud where files will be uploaded

  // Initialize NextCloud connector
  private lazy val nextcloud = new NextcloudConnector(nextcloudUrl, nextcloudUser, nextcloudPassword)

  def fileToUrl(file: File, errorMessage: String => Unit): String = {
    // Run in background thread to avoid blocking UI
    logger.debug(s"Processing ${file.getName}...")

    val basename = FilenameUtils.getBaseName(file.getName)
    val extension = FilenameUtils.getExtension(file.getName)
    val unique = Random.between(1, Int.MaxValue).toHexString
    val targetName = s"$basename-$unique.$extension"

    logger.debug("File copied, uploading to NextCloud...")

    // Upload to NextCloud using the API
    val remotePath = s"$nextcloudRemotePath/$targetName"
    nextcloud.uploadFile(file, remotePath)

    logger.debug("File uploaded, creating share link...")

    // Create share link
    val share = nextcloud.doShare(
      remotePath,
      ShareType.PUBLIC_LINK,
      null, // shareWith (not needed for public links)
      false, // publicUpload
      null, // password
      SharePermissions(SharePermissions.SingleRight.READ, SharePermissions.SingleRight.SHARE) // permissions
    )

    val shareUrl = share.getUrl

    shareUrl
  }

  private val logger = Logger[Nextcloud.type]
}
