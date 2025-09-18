package de.unruh.quickfind
package items

import core.{Item, LeafItem, SVGImage, ScalableImage, SnippetPreviewItem, Utils}

import java.net.URL

class Link(val parent: Item, url: URL, preview: Option[(String,String,String)])
  extends SnippetPreviewItem(preview), LeafItem{
  override def title: String = url.toString
  override def icon: ScalableImage = Link.icon
  override def defaultAction(): Unit =
    Utils.showInBrowser(url)

  override def toString: String = s"[Link $url]"

  override val persistentKey: Array[Byte] = url.toString.getBytes
}

object Link {
  val icon: SVGImage = SVGImage.fromResource("/icons/browser-google-chrome-svgrepo-com.svg")
}