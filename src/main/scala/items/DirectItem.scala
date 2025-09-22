package de.unruh.quickfind
package items

import core.{ChildItem, Item}

import de.unruh.quickfind.items.ShellCommand.trusted

import java.lang.reflect.Constructor

abstract class DirectItem extends ChildItem {

}

object DirectItem {
  def instantiate(parent: Item, clazz: String, trust: trusted.type): DirectItem = {
    val clazzObj = Class.forName(clazz)
    if (!classOf[DirectItem].isAssignableFrom(clazzObj))
      throw RuntimeException(s"Explicitly configured item class $clazz is does not extend DirectItem")
    val constructors = clazzObj.getConstructors
    assert(constructors.nonEmpty)
    val constructor = constructors(0).asInstanceOf[Constructor[DirectItem]]

    val parameters = for (parameter <- constructor.getParameters) yield
      parameter.getName match
        case "parent" => parent
        case name => throw RuntimeException(s"Class $clazz has constructor with invalid parameter $name")

    constructor.newInstance(parameters*)
  }


}