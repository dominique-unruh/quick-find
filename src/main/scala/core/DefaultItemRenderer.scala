package de.unruh.quickfind
package core

import java.awt.{Color, Component, Dimension, Font, Graphics}
import javax.swing.border.EmptyBorder
import javax.swing.*
import scala.util.boundary
import scala.util.boundary.break

class DefaultItemRenderer(loadingItem: Item) extends ListCellRenderer[Item] {
  import DefaultItemRenderer._

  private val defaultListCellRenderer = new DefaultListCellRenderer()
  private val component = JPanel()
  private val titleLabel = JLabel()
  private val previewLabel = JLabel()
  private var icon: ScalableImage = Item.defaultIcon
  private val loadingComponent = JPanel()
  initialize()

  private def initialize(): Unit = {
    component.setLayout(BoxLayout(component, BoxLayout.X_AXIS))
    component.setBorder(EmptyBorder(4, 2, 4, 2))
    val textBox = Box.createVerticalBox().nn
    textBox.add(titleLabel)
    textBox.add(previewLabel)
    val iconPanel = new JPanel {
      override def paintComponent(g: Graphics): Unit =
        super.paintComponent(g)
        g.drawImage(icon.getImageAtSize(getWidth, getHeight), 0, 0, (_, _, _, _, _, _) => false)

      override def getPreferredSize: Dimension = {
        val height = textBox.getPreferredSize.nn.height
        Dimension(height, height)
      }

      override def getMaximumSize: Dimension = getPreferredSize
      override def getMinimumSize: Dimension = getPreferredSize
    }
    iconPanel.setOpaque(false)

    previewLabel.setFont(previewLabel.getFont.nn.deriveFont(Font.PLAIN).nn.deriveFont(Constants.fontSize.toFloat))
    titleLabel.setFont(titleLabel.getFont.nn.deriveFont(Font.BOLD).nn.deriveFont(Constants.fontSize.toFloat))
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
    val path = {
      val builder = Seq.newBuilder[String]
      var current = item
      boundary { while (true) {
        builder += current.title
        current.parentOption match {
          case Some(value) => current = value
          case None => break()
        }
      } }
      builder.result().reverse
    }

    val string = path.mkString(s" ${Constants.separator} ")
    if item.isFolder then
      string + s" ${Constants.separator}"
    else
      string
  }

  private def nonEmtpyString(string: String) =
    if (string.isEmpty) " " else string

  override def getListCellRendererComponent(list: JList[_ <: Item], item: Item, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component =
    if (item eq loadingItem)
      loadingComponent
    else {
      val bgColor =
        if (isSelected) selectedCellColor
        else if (index % 2 == 0) evenCellColor
        else oddCellColor
      component.setBackground(bgColor)
      titleLabel.setText(nonEmtpyString(title(item))) // Ensure the label has height even if empty
      icon = item.icon
      previewLabel.setText(nonEmtpyString(item.previewLine)) // Ensure the label has height even if empty
      component
    }
}

object DefaultItemRenderer {
  val oddCellColor: Color = Color.white.nn
  val evenCellColor: Color = Color.lightGray.nn
  val selectedCellColor: Color = Color(200, 200, 255)
  val loadingImage: SVGImage = SVGImage.fromResource("/icons/loading-svgrepo-com.svg")
}