package de.unruh.quickfind
package core

import DefaultItemRenderer.*
import SearchWindow.*

import java.awt.{BorderLayout, KeyboardFocusManager, Toolkit}
import java.awt.event.KeyEvent
import java.util
import javax.swing.{Box, JFrame, JLabel, JPanel, JTextField, WindowConstants}
import javax.swing.event.{DocumentEvent, DocumentListener}
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable
import scala.ref.WeakReference

/** The main window of the app. */
class SearchWindow(root: Item) extends JFrame {
  assert(root.isFolder)
  private val prefix = new JLabel()
  private val input = new JTextField()
  private val results = new InfiniteList[Item](DefaultItemRenderer(root, loadingItem), loadingItem)
  private final case class SearchIndexFolder(searchString: String, index: Int, folder: Item)
  private val searchStack = mutable.Stack[SearchIndexFolder]()
  private val recursiveChildrenCache =
    new util.IdentityHashMap[Item, WeakReference[Iterable[Item]]].asScala
  initialize()

  private def getChildren(folder: Item): Iterable[Item] = {
    def getValue = recursiveChildrenCache.get(folder).flatMap(_.get)

    getValue match {
      case Some(value) => value
      case None => synchronized {
        getValue match {
          case Some(value) => value
          case None =>
            println(s"Creating sorted children list for $folder")
            val builder = Seq.newBuilder[Item]
            folder.addChildren(builder)
            val seq = builder.result()
            val sortedSeq = seq.sortBy(_.weight)
            // TODO Using WeakReferences probably rebuilds root too often
            recursiveChildrenCache.put(folder, WeakReference(sortedSeq))
            seq
        }
      }
    }
  }

  private def filter(): Unit = {
    val search = input.getText.nn.toLowerCase
    val iterator =
      for (child <- getChildren(currentFolder).iterator;
           if child.title.toLowerCase.nn.indexOf(search) != -1)
        yield child
    results.setGenerator(iterator)
  }

  private def currentFolder: Item = if searchStack.isEmpty then root else searchStack.head.folder

  private def tabPressed(): Unit = {
    val index = results.selected
    val item = results(index)
    if (item.isFolder)
      pushFolder(input.getText.nn, index, item)
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

  private def updatePrefix(): Unit = {
    if (searchStack.isEmpty)
      prefix.setText("")
    else
      val str = searchStack.reverseIterator.map(_.searchString).mkString("", s" ${Constants.separator} ", s" ${Constants.separator}")
      prefix.setText(str)
    results.setRenderer(DefaultItemRenderer(currentFolder, loadingItem))
  }

  private def downPressed(): Unit =
    results.selectRelative(1)

  private def upPressed(): Unit =
    results.selectRelative(-1)

  private def pageDownPressed(): Unit =
    results.selectRelative(10)

  private def pageUpPressed(): Unit =
    results.selectRelative(-10)

  private def escPressed(): Unit = {
    if (searchStack.isEmpty && input.getText.nn.isEmpty)
      close()
    else if (searchStack.isEmpty)
      input.setText("")
      filter()
    else
      popFolder()
  }

  private def enterPressed(): Unit = try {
    val item = results.selectedItem
    // If we close() after the defaultAction, then showInEmacs does not raise the Emacs frame, maybe due to some race condition with focus change?
    close()
    item.defaultAction()
  } catch
    case _: NoSuchElementException =>

  private def shiftTabPressed(): Unit =
    popFolder()

  private def initialize(): Unit = {
    setTitle("Quick Find")
    val panel = new JPanel()
    val box = Box.createHorizontalBox().nn
    panel.setLayout(new BorderLayout())
    box.add(prefix)
    box.add(input)
    panel.add(box, BorderLayout.NORTH)
    panel.add(results, BorderLayout.CENTER)
    input.getDocument.nn.addDocumentListener(new DocumentListener {
      override def insertUpdate(documentEvent: DocumentEvent): Unit = filter()
      override def removeUpdate(documentEvent: DocumentEvent): Unit = filter()
      override def changedUpdate(documentEvent: DocumentEvent): Unit = filter()
    })
    input.setFont(input.getFont.nn.deriveFont(Constants.fontSize.toFloat))
    prefix.setFont(prefix.getFont.nn.deriveFont(Constants.fontSize.toFloat))
    add(panel)
    KeyboardFocusManager.getCurrentKeyboardFocusManager.nn.addKeyEventDispatcher((event: KeyEvent | Null) => event.nn.getID match
      case KeyEvent.KEY_PRESSED => event.nn.getKeyCode match
        case KeyEvent.VK_TAB if event.nn.isShiftDown => shiftTabPressed(); true
        case KeyEvent.VK_TAB => tabPressed(); true
        case KeyEvent.VK_ESCAPE => escPressed(); true
        case KeyEvent.VK_DOWN => downPressed(); true
        case KeyEvent.VK_UP => upPressed(); true
        case KeyEvent.VK_PAGE_UP => pageUpPressed(); true
        case KeyEvent.VK_PAGE_DOWN => pageDownPressed(); true
        case KeyEvent.VK_ENTER => enterPressed(); true
        case KeyEvent.VK_W if event.nn.isControlDown => close(); true
        case KeyEvent.VK_Q if event.nn.isControlDown => close(); true
        case _ => false
      case _ => false)
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    val screenWidth = Toolkit.getDefaultToolkit.nn.getScreenSize.nn.width
    setSize(screenWidth / 2, screenWidth / 4)
    setLocationRelativeTo(null)
    setUndecorated(true)
  }

  private def close(): Unit = setVisible(false)

  /** Show the window, with reset search input. */
  def activate(): Unit = {
    searchStack.clear()
    input.setText("")
    input.requestFocusInWindow()
    updatePrefix()
    setVisible(true)
    filter()
  }
}

object SearchWindow {
  // TODO equals, hashCode
  private object loadingItem extends Item {
    override val parentOption: Option[Item] = None
    override val children: List[ChildItem] = Nil
    override def defaultAction(): Unit = {}
    override val previewLine = ""
    override val title = "Loading..."
    override val icon: ScalableImage = Item.defaultIcon
    override val persistentKey: String = "LOADING ITEM"
  }

//  private val loadingItemPath = ItemPath(loadingItem)
}



