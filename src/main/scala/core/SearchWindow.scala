package de.unruh.quickfind
package core

import java.awt.event.KeyEvent
import java.awt.{BorderLayout, KeyEventDispatcher, KeyboardFocusManager}
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.*
import scala.collection.mutable

/** The main window of the app. */
class SearchWindow(root: Item) extends JFrame {
  assert(root.isFolder)
  private val prefix = new JLabel()
  private val input = new JTextField()
  private val results = new InfiniteList[ItemPath](itemPathRenderer)
  private final case class SearchIndexFolder(searchString: String, index: Int, folder: Item)
  private val searchStack = mutable.Stack[SearchIndexFolder]()
  initialize()

  private object itemPathRenderer extends InfiniteList.Renderer[ItemPath] {
    override def label(path: ItemPath): String = {
      val string = path.map(_.text).mkString(s" ${Constants.separator} ")
      if path.last.isFolder then
        string + s" ${Constants.separator}"
      else
        string
    }
  }

  private def filter(): Unit = {
    val search = input.getText.toLowerCase
    val iterator =
      for (path <- currentFolder.recursiveIterator
           if path.last.text.toLowerCase.indexOf(search) != -1)
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
    setSize(400, 400)
    setLocationRelativeTo(null)
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
