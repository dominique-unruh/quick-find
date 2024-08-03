package de.unruh.quickfind
package core

import java.awt.event.KeyEvent
import java.awt.{BorderLayout, KeyEventDispatcher, KeyboardFocusManager}
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.*
import scala.collection.mutable

class SearchWindow(root: Folder) extends JFrame {
  private val prefix = new JLabel()
  private val input = new JTextField()
  private val results = new InfiniteList[ItemPath](itemPathRenderer)
  private final case class SearchIndexFolder(searchString: String, index: Int, folder: Folder)
  private val searchStack = mutable.Stack[SearchIndexFolder]()
  initialize()

  private object itemPathRenderer extends InfiniteList.Renderer[ItemPath] {
    override def label(path: ItemPath): String = {
      val string = path.map(_.text).mkString(s" ${Constants.separator} ")
      if path.item.isInstanceOf[Folder] then
        string + s" ${Constants.separator}"
      else
        string
    }
  }

  private def filter(): Unit = {
    val search = input.getText.toLowerCase
    val iterator =
      for (path <- currentFolder.recursiveIterator
           if path.item.text.toLowerCase.indexOf(search) != -1)
        yield path
    results.setGenerator(iterator)
  }

  private def currentFolder: Folder = if searchStack.isEmpty then root else searchStack.head.folder

  private def tabPressed(): Unit =
    val index = results.selected
    val path = results(index)
    path.item match
      case folder: Folder =>
        pushFolder(input.getText, index, folder)
      case item =>

  private def pushFolder(searchString: String, index: Int, folder: Folder): Unit =
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
    results.selectNext()

  private def upPressed(): Unit =
    results.selectPrevious()

  private def escPressed(): Unit = {
    if (searchStack.isEmpty)
      close()
    else
      popFolder()
  }

  private def enterPressed(): Unit =
    val path = results.selectedItem
    path.item.defaultAction()
    close()

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
        case KeyEvent.VK_ENTER => enterPressed(); true
        case KeyEvent.VK_W if event.isControlDown => close(); true
        case _ => false
      case _ => false)
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    setSize(400, 400)
    setLocationRelativeTo(null)
  }

  private def close(): Unit = setVisible(false)

  def activate(): Unit = {
    searchStack.clear()
    input.setText("")
    updatePrefix()
    setVisible(true)
    filter()
  }
}
