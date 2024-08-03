package de.unruh.quickfind
package core

import java.awt.Component
import java.awt.event.{AdjustmentEvent, AdjustmentListener}
import java.util.concurrent.LinkedBlockingQueue
import javax.swing.SwingUtilities.invokeLater
import javax.swing.{DefaultListCellRenderer, DefaultListModel, JLabel, JList, JScrollPane, ListCellRenderer, SwingUtilities}

/*
final class PathListItem(val path: ItemPath) extends ListItem:
  override def listItemLabel: String =
    val string = path.map(_.text).mkString(s" ${Constants.separator} ")
    if path.item.isInstanceOf[Folder] then
      string + s" ${Constants.separator}"
    else
      string
*/

/**
 * Swing component displaying an infinite scrolling list.
 * The content comes from an Iterator that is read on demand.
 * @tparam A type of the items
 */
class InfiniteList[A](renderer: InfiniteList.Renderer[A]) extends JScrollPane {
  private val list = new JList[A]()
  private var pullingThread: Thread = _
  private var targetLengthQueue = new LinkedBlockingQueue[Int]()
  private var intendedSelection = 0
  initialize()

  /** Set new content. */
  def setGenerator(generator: Iterator[A]): Unit = synchronized {
    if pullingThread != null then
      pullingThread.interrupt()
    val model = DefaultListModel[A]
    targetLengthQueue = new LinkedBlockingQueue[Int]()
    list.setModel(model)
    setIntendedSelection(0)
    pullingThread = new Thread(() => pull(generator, model, targetLengthQueue))
    pullingThread.start()
    reachedBottom()
  }

  private def pull(generator: Iterator[A], model: DefaultListModel[A], targetQueue: LinkedBlockingQueue[Int]): Unit = {
    var count = 0
    try {
      while (generator.hasNext) {
        val targetLength = targetQueue.take()
//        println(s"Pulling till $targetLength")
        while (count < targetLength && generator.hasNext) {
          val element = generator.next()
//          println(s"Adding element $element (${model.size()}/$targetLength)")
          count += 1
          invokeLater(() => appendElement(model, element))
        }
      }
    } catch
      case _: InterruptedException =>
  }

  /** Must be called in "invokeLater" thread */
  private def appendElement(model: DefaultListModel[A], element: A): Unit = {
    model.addElement(element)
    setSelection(intendedSelection)
    SwingUtilities.invokeLater { () =>
      val bar = getVerticalScrollBar
      if bar.getMaximum == bar.getValue + bar.getVisibleAmount then
        reachedBottom()
    }
  }

  private def reachedBottom(): Unit =
    targetLengthQueue.put(list.getModel.getSize + 20)

  private object listCellRenderer extends DefaultListCellRenderer {
    val label = new JLabel()

    override def getListCellRendererComponent(list: JList[_], value: Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component = {
      val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).asInstanceOf[JLabel]
      val item = value.asInstanceOf[A]
      label.setText(renderer.label(item))
      label
    }
  }

  private def initialize(): Unit = {
    list.setCellRenderer(listCellRenderer)

    getViewport.add(list)

    // Add scroll listener to load more items when reaching the bottom
    getVerticalScrollBar.addAdjustmentListener(new AdjustmentListener() {
      override def adjustmentValueChanged(e: AdjustmentEvent): Unit = {
        if (!e.getValueIsAdjusting && e.getAdjustable.getMaximum == e.getValue + e.getAdjustable.getVisibleAmount)
          SwingUtilities.invokeLater(() => reachedBottom())
      }
    })

    reachedBottom()
  }

  /** Returns the index of the selected item.
   * The list makes sure that there always is a selected item,
   * unless it's empty (in which case -1 is returned). */
  def selected: Int = list.getSelectedIndex match
    case -1 => if (list.getModel.getSize > 0) 0 else -1
    case index => index

  /** Returns the selected item.
   * @throws NoSuchElementException if the list is empty */
  def selectedItem: A = {
    val index = selected
    if (index == -1) throw new NoSuchElementException()
    apply(selected)
  }

  /** Sets the "intended" selection.
   * If `<= index` elements have been be loaded so far from the generator,
   * the last one will be selected, but the selection will be updated until
   * it matches the intended selection.
   * */
  def setIntendedSelection(index: Int): Unit = {
    intendedSelection = index
    setSelection(index)
  }

  /** Returns the `index`-th element of the list. */
  def apply(index: Int): A = list.getModel.getElementAt(index)

  private def setSelection(index: Int): Unit = {
    if (index < 0)
      list.setSelectedIndex(0)
    else if (index >= list.getModel.getSize)
      list.setSelectedIndex(list.getModel.getSize - 1)
    else
      list.setSelectedIndex(index)
    ensureSelectionVisible()
  }

  /** Moves the selection up by one item.
   * If we are on the top, nothing happens.
   * (Will also adjust the intended selection.) */
  def selectNext(): Unit =
    setIntendedSelection(selected + 1)

  /** Moves the selection down by one item.
   * If we are on the bottom, nothing happens.
   * (Will also adjust the intended selection.) */
  def selectPrevious(): Unit =
    setIntendedSelection(selected - 1)

  private def ensureSelectionVisible(): Unit =
    list.ensureIndexIsVisible(list.getSelectedIndex)
}

object InfiniteList {
  trait Renderer[A] {
    def label(item: A): String
  }
}