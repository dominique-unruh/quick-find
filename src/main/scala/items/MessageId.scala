package de.unruh.quickfind
package items

import core.{Item, LeafItem, SVGImage, ScalableImage, SnippetPreviewItem}

class MessageId(val parent: Item, address: String, preview: Option[(String, String, String)])
  extends SnippetPreviewItem(preview), LeafItem {
  override def icon: ScalableImage = MessageId.icon
  override def title: String = address
  override def defaultAction(): Unit =
    import sys.process._
    val command = Seq("/usr/bin/thunderbird", s"mid:$address")
    println(s"Running: ${command.mkString(" ")}")
    command.run()

  override val persistentKey: Array[Byte] = address.getBytes
}

object MessageId {
  val icon: SVGImage = SVGImage.fromResource("/icons/email-notification-message-envelope-letter-chat-svgrepo-com.svg")
}