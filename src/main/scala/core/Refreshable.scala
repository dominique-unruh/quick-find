package de.unruh.quickfind
package core

import de.unruh.quickfind.core.Refreshable.cache

import java.lang.ref.WeakReference
import scala.collection.mutable

trait Refreshable extends Item {
  private var _children: Seq[Item] = _
  private var lastCheck: Long = 0
  protected val refreshTimeout: Long = 10000
  final override def children: Iterable[Item] = {
    val currentTime = System.currentTimeMillis()
    if (currentTime > lastCheck + refreshTimeout) {
      synchronized {
        if (currentTime > lastCheck + refreshTimeout) {
          if (_children == null  ||  needsRefresh) {
            _children = {
              for (child <- loadChildren.iterator) yield
                cache.get(child) match
                  case Some(ref) => ref.get match
                    case null => cache.put(child, WeakReference(child)); child
                    case cachedChild => cachedChild
                  case None => cache.put(child, WeakReference(child)); child
            }.toSeq
          }
          lastCheck = System.currentTimeMillis()
        }
      }
    }
    _children
  }

  def needsRefresh: Boolean
  def loadChildren: IterableOnce[Item]

}

object Refreshable {
  private [Refreshable] val cache = mutable.WeakHashMap[Item, WeakReference[Item]]()
}