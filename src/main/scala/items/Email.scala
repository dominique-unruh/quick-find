package de.unruh.quickfind
package items

import core.{Item, LeafItem, SVGImage, ScalableImage, SnippetPreviewItem}

import java.io.ObjectInputStream

class Email(address: String, preview: Option[(String,String,String)])
  extends SnippetPreviewItem(preview), LeafItem {
  override def title: String = address

  override def defaultAction(): Unit =
    import sys.process._
    Seq("thunderbird", "-compose", s"to=$address").run()

  override def icon: ScalableImage = Email.icon
}

object Email {
  // TODO implement this
  def isMessageId(address: String): Boolean = true
  def icon: ScalableImage = SVGImage.fromResource("/icons/at-sign-svgrepo-com.svg")
}
