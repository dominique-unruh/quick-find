package de.unruh.quickfind
package core

import java.awt.{Color, Component, Dimension, Font, Graphics}
import javax.swing.border.EmptyBorder
import javax.swing.*
import scala.util.boundary
import scala.util.boundary.break

class DefaultItemRenderer(rootItem: Item, loadingItem: Item) extends ListCellRenderer[Item] {
  import DefaultItemRenderer._

//  private val defaultListCellRenderer = new DefaultListCellRenderer()
  private val component = JPanel()
  private val titleLabel = MiddleTruncateLabel()
  private val previewLabel = JLabel()
  private var icon: ScalableImage = Item.defaultIcon
  private val loadingComponent = JPanel()
  initialize()

  private def initialize(): Unit = {
    component.setLayout(BoxLayout(component, BoxLayout.X_AXIS))
    component.setBorder(EmptyBorder(4, 2, 4, 2))
    val textBox = Box.createVerticalBox()
    textBox.add(titleLabel)
    textBox.add(previewLabel)
    val iconPanel = new JPanel {
      override def paintComponent(g: Graphics): Unit =
        super.paintComponent(g)
        g.drawImage(icon.getImageAtSize(getWidth, getHeight), 0, 0, (_, _, _, _, _, _) => false)

      override def getPreferredSize: Dimension = {
        val height = textBox.getPreferredSize.height
        Dimension(height, height)
      }

      override def getMaximumSize: Dimension = getPreferredSize
      override def getMinimumSize: Dimension = getPreferredSize
    }
    iconPanel.setOpaque(false)

    previewLabel.setFont(previewLabel.getFont.deriveFont(Font.PLAIN).deriveFont(Constants.fontSize.toFloat))
    titleLabel.setFont(titleLabel.getFont.deriveFont(Font.BOLD).deriveFont(Constants.fontSize.toFloat))
    component.add(Box.createHorizontalStrut(5))
    component.add(iconPanel)
    component.add(Box.createHorizontalStrut(5))
    component.add(textBox)
    component.setOpaque(true)

    val loadingIcon: JPanel = new JPanel {
      override def paintComponent(g: Graphics): Unit =
        super.paintComponent(g)
        g.drawImage(loadingImage.getImageAtSize(getWidth, getHeight), 0, 0, (_, _, _, _, _, _) => false)
      override val getPreferredSize: Dimension = Dimension(50, 50)
      override val getMaximumSize: Dimension = getPreferredSize
      override val getMinimumSize: Dimension = getPreferredSize
    }
    loadingComponent.add(loadingIcon)
  }

  private def title(item: Item): String = {
    val path = item.pathTo(rootItem).map(_.title)

    val string = path.mkString(s" ${Constants.separator} ")
    if item.isFolder then
      string + s" ${Constants.separator}"
    else
      string
  }

  private def nonEmptyString(string: String) =
    if (string.isEmpty) " " else string

  override def getListCellRendererComponent(list: JList[? <: Item], item: Item, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component =
    if (item eq loadingItem)
      loadingComponent
    else {
      val bgColor =
        if (isSelected) selectedCellColor
        else if (index % 2 == 0) evenCellColor
        else oddCellColor
      component.setBackground(bgColor)
//      val truncatedTitle = truncateMiddle(nonEmptyString(title(item)), titleLabel)
//      titleLabel.setText(truncatedTitle)
      titleLabel.setText(nonEmptyString(title(item)))
      icon = item.icon
      previewLabel.setText(nonEmptyString(item.previewLine)) // Ensure the label has height even if empty
      component
    }

/*  private def truncateMiddle(text: String, label: JLabel): String = {
    if (text.isEmpty) return text

    val metrics = label.getFontMetrics(label.getFont)

    // Try to get available width, fall back to a reasonable default if not available
    //    val labelWidth = if (label.getWidth > 0) label.getWidth else label.getPreferredSize.width

    val availableWidth = label.getPreferredSize.width - label.getInsets.left - label.getInsets.right
    println(s"availableWidth: $availableWidth")

    // If width is not available or text fits, return as is
    if (availableWidth <= 0 || metrics.stringWidth(text) <= availableWidth)
      return text

    val ellipsis = "…" // Unicode horizontal ellipsis character

    boundary[String] {
      var max = (text.length + 1) / 2 // Always a value that doesn't fit
      var min = 0 // Always a value that fits
      while (true) {
        val test = min + (max - min) / 2
        val truncated = text.take(test) + ellipsis + text.takeRight(test)
        val fits = metrics.stringWidth(truncated) <= availableWidth
        if (fits)
          min = test
        else
          max = test
        if (max <= min + 1)
          break(truncated)
      }
      Utils.unreachable
    }
  }*/

}

object DefaultItemRenderer {
  val oddCellColor: Color = Color.white
  val evenCellColor: Color = Color.lightGray
  val selectedCellColor: Color = Color(200, 200, 255)
  val loadingImage: SVGImage = SVGImage.fromResource("/icons/loading-svgrepo-com.svg")
}


import javax.swing._
import java.awt._

class MiddleTruncateLabel extends JLabel {
  // Keep the text that was set using setText because we temporarily overwrite it in paintComponent
  private var originalText: String = ""

  override def setText(text: String): Unit = {
    originalText = if (text == null) "" else text
//    originalText = "this is a little test this is a little test this is a little test this is a little test this is a little test this is a little test"
    super.setText(originalText)
  }

  override def paintComponent(g: Graphics): Unit = {
    if (originalText.nonEmpty) {
      val metrics = g.getFontMetrics
      val availableWidth = g.getClipBounds.x + g.getClipBounds.width - getInsets.left - getInsets.right // - 10 // not sure why we need to subtrace more than the insets
      val truncatedText = truncateMiddleForWidth(originalText, metrics, availableWidth)

      if (truncatedText != getText) {
        super.setText(truncatedText)
      }
    }
    super.paintComponent(g)
    if (originalText != getText)
      super.setText(originalText)
  }

  private def truncateMiddleForWidth(text: String, metrics: FontMetrics, availableWidth: Int): String = {
    if (text.isEmpty) return text

//    println((availableWidth, metrics.stringWidth(text)))

    // If width is not available or text fits, return as is
    if (availableWidth <= 0 || metrics.stringWidth(text) <= availableWidth)
      return text

    val ellipsis = "…" // Unicode horizontal ellipsis character

    val len = boundary[Int] {
      var max = (text.length + 1) / 2 // Always a value that doesn't fit
      var min = 0 // Always a value that fits
      while (true) {
        val test = min + (max - min) / 2
        val truncated = text.take(test) + ellipsis + text.takeRight(test)
        val fits = metrics.stringWidth(truncated) <= availableWidth
        if (fits)
          min = test
        else
          max = test
        if (max <= min + 1)
          break(min)
      }
      Utils.unreachable
    }

    text.take(len) + ellipsis + text.takeRight(len)
  }
}

