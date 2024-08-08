package de.unruh.quickfind
package items

import core.{LeafItem, SVGImage, ScalableImage, SnippetPreviewItem}

class MessageId(address: String, preview: Option[(String, String, String)])
  extends SnippetPreviewItem(preview), LeafItem {
  override def icon: ScalableImage = MessageId.icon
  override def title: String = address
  override def defaultAction(): Unit =
    import sys.process._
    val command = Seq("/opt/cb_thunderlink/cb_thunderlink", s"thunderlink://messageid=$address")
    println(s"Running: ${command.mkString(" ")}")
    command.run()
}

object MessageId {
  val icon: SVGImage = SVGImage.fromResource("/icons/email-notification-message-envelope-letter-chat-svgrepo-com.svg")
}