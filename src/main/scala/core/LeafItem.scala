package de.unruh.quickfind
package core

trait LeafItem extends Item {
  override def children: Iterable[Item] = Seq.empty
  override def isFolder: Boolean = false
}
