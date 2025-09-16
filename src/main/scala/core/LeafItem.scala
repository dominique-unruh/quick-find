package de.unruh.quickfind
package core

trait LeafItem extends Item {
  override val children: Iterable[ChildItem] = Seq.empty
  override val isFolder: Boolean = false
}
