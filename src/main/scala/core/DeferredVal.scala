package de.unruh.quickfind
package core

import scala.annotation.targetName
import scala.concurrent.Promise

class DeferredVal[T](using name: sourcecode.Name, manager: DeferredVal.Manager) {
  manager.register(this)
  private val promise = Promise[T]
  private def validKey(key: DeferredVal.Key | DeferredVal.Manager) = key match
    case manager2 : DeferredVal.Manager =>
      manager2.key eq manager.key
    case _ => key eq manager.key
  @targetName("set")
  def :=(value: T)(using key: DeferredVal.Key | DeferredVal.Manager): Unit =
    if (!validKey(key))
      throw IllegalAccessException(s"Writing variable ${name.value} with incorrect key")
    promise.success(value)
  def initialized: Boolean = promise.isCompleted
  inline def get: T = promise.future.value.getOrElse(throw IllegalStateException(s"Accessing uninitialized variable ${name.value}")).get
  def assertInitialized(): Unit =
    if (!promise.isCompleted)
      throw IllegalStateException(s"Variable ${name.value} not initialized")
}

object DeferredVal {
  given unwrap[T]: Conversion[DeferredVal[T], T] = _.get

  trait Manager {
    private [DeferredVal] def register(variable: DeferredVal[?]): Unit
    val key: Key = new Key
  }

  def assertInitialized()(using manager: CheckInitManager): Unit =
    manager.assertInitialized()

  class SimpleManager extends Manager {
    override private[DeferredVal] def register(variable: DeferredVal[?]): Unit = {}
  }

  class CheckInitManager extends Manager {
    private val variables = Iterable.newBuilder[DeferredVal[?]]
    override private[DeferredVal] def register(variable: DeferredVal[?]): Unit =
      variables += variable
    def assertInitialized(): Unit =
      try
        for (variable <- variables.result())
          variable.assertInitialized()
      finally
        variables.clear()
  }

  class Key
}
