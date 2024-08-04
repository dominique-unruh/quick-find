package de.unruh.quickfind
package core

import DefaultItemPathRenderer.*

import java.awt.event.KeyEvent
import java.awt.*
import javax.swing.border.EmptyBorder
import javax.swing.{Box, BoxLayout, DefaultListCellRenderer, JFrame, JLabel, JList, JPanel, JTextField, ListCellRenderer, WindowConstants}
import javax.swing.event.{DocumentEvent, DocumentListener}
import scala.collection.mutable

/** The main window of the app. */
class SearchWindow(root: Item) extends JFrame {
  assert(root.isFolder)
  private val prefix = new JLabel()
  private val input = new JTextField()
  private val results = new InfiniteList[ItemPath](DefaultItemPathRenderer())
  private final case class SearchIndexFolder(searchString: String, index: Int, folder: Item)
  private val searchStack = mutable.Stack[SearchIndexFolder]()
  initialize()

  private def filter(): Unit = {
    val search = input.getText.toLowerCase
    val iterator =
      for (path <- currentFolder.recursiveIterator
           if path.last.title.toLowerCase.indexOf(search) != -1)
        yield path
    results.setGenerator(iterator)
  }

  private def currentFolder: Item = if searchStack.isEmpty then root else searchStack.head.folder

  private def tabPressed(): Unit = {
    val index = results.selected
    val path = results(index)
    if (path.last.isFolder)
      pushFolder(input.getText, index, path.last)
  }

  private def pushFolder(searchString: String, index: Int, folder: Item): Unit =
    val sif = SearchIndexFolder(searchString, index, folder)
    searchStack.push(sif)
    updatePrefix()
    input.setText("")
    filter()

  private def popFolder(): Unit =
    if (searchStack.nonEmpty) {
      val sif = searchStack.pop()
      updatePrefix()
      input.setText(sif.searchString)
      filter()
      results.setIntendedSelection(sif.index)
    }

  private def updatePrefix(): Unit =
    if (searchStack.isEmpty)
      prefix.setText("")
    else
      val str = searchStack.reverseIterator.map(_.searchString).mkString("", s" ${Constants.separator} ", s" ${Constants.separator}")
      prefix.setText(str)

  private def downPressed(): Unit =
    results.selectRelative(1)

  private def upPressed(): Unit =
    results.selectRelative(-1)

  private def pageDownPressed(): Unit =
    results.selectRelative(10)

  private def pageUpPressed(): Unit =
    results.selectRelative(-10)

  private def escPressed(): Unit = {
    if (searchStack.isEmpty && input.getText.isEmpty)
      close()
    else if (searchStack.isEmpty)
      input.setText("")
      filter()
    else
      popFolder()
  }

  private def enterPressed(): Unit = try {
    val path = results.selectedItem
    path.last.defaultAction()
    close()
  } catch
    case _: NoSuchElementException =>

  private def shiftTabPressed(): Unit =
    popFolder()

  private def initialize(): Unit = {
    setTitle("Quick Find")
    val panel = new JPanel()
    val box = Box.createHorizontalBox()
    panel.setLayout(new BorderLayout())
    box.add(prefix)
    box.add(input)
    panel.add(box, BorderLayout.NORTH)
    panel.add(results, BorderLayout.CENTER)
    input.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(documentEvent: DocumentEvent): Unit = filter()
      override def removeUpdate(documentEvent: DocumentEvent): Unit = filter()
      override def changedUpdate(documentEvent: DocumentEvent): Unit = filter()
    })
    input.setFont(input.getFont.deriveFont(Constants.fontSize.toFloat))
    add(panel)
    KeyboardFocusManager.getCurrentKeyboardFocusManager.addKeyEventDispatcher((event: KeyEvent) => event.getID match
      case KeyEvent.KEY_PRESSED => event.getKeyCode match
        case KeyEvent.VK_TAB if event.isShiftDown => shiftTabPressed(); true
        case KeyEvent.VK_TAB => tabPressed(); true
        case KeyEvent.VK_ESCAPE => escPressed(); true
        case KeyEvent.VK_DOWN => downPressed(); true
        case KeyEvent.VK_UP => upPressed(); true
        case KeyEvent.VK_PAGE_UP => pageUpPressed(); true
        case KeyEvent.VK_PAGE_DOWN => pageDownPressed(); true
        case KeyEvent.VK_ENTER => enterPressed(); true
        case KeyEvent.VK_W if event.isControlDown => close(); true
        case KeyEvent.VK_Q if event.isControlDown => close(); true
        case _ => false
      case _ => false)
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    val screenWidth = Toolkit.getDefaultToolkit.getScreenSize.width
    setSize(screenWidth / 2, screenWidth / 4)
    setLocationRelativeTo(null)
    setUndecorated(true)
  }

  private def close(): Unit = setVisible(false)

  /** Show the window, with reset search input. */
  def activate(): Unit = {
    searchStack.clear()
    input.setText("")
    updatePrefix()
    setVisible(true)
    filter()
  }
}

class DefaultItemPathRenderer extends ListCellRenderer[ItemPath] {
  private val defaultListCellRenderer = new DefaultListCellRenderer()
  private val component = JPanel()
  private val titleLabel = JLabel()
  private val previewLabel = JLabel()
  private var icon: ScalableImage = _
  initialize()

  private def initialize(): Unit = {
    component.setLayout(BoxLayout(component, BoxLayout.X_AXIS))
    component.setBorder(EmptyBorder(4,2,4,2))
    val textBox = Box.createVerticalBox()
    textBox.add(titleLabel)
    textBox.add(previewLabel)
    val iconPanel = new JPanel {
      override def paintComponent(g: Graphics): Unit =
        super.paintComponent(g)
        if (icon != null)
          g.drawImage(icon.getImageAtSize(getWidth, getHeight), 0, 0, (_,_,_,_,_,_) => false)
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
  }

  private def title(path: ItemPath): String = {
    val string = path.map(_.title).mkString(s" ${Constants.separator} ")
    if path.last.isFolder then
      string + s" ${Constants.separator}"
    else
      string
  }

  private def nonEmtpyString(string: String) =
    if (string.isEmpty) " " else string

  override def getListCellRendererComponent(list: JList[_ <: ItemPath], item: ItemPath, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component = {
    val bgColor =
      if (isSelected) selectedCellColor
      else if (index % 2 == 0) evenCellColor
      else oddCellColor
    component.setBackground(bgColor)
    titleLabel.setText(nonEmtpyString(title(item))) // Ensure the label has height even if empty
    icon = item.last.icon
    previewLabel.setText(nonEmtpyString(item.last.previewLine)) // Ensure the label has height even if empty
    component
  }
}

object DefaultItemPathRenderer {
  val oddCellColor: Color = Color.white
  val evenCellColor: Color = Color.lightGray
  val selectedCellColor: Color = Color(200,200,255)
}