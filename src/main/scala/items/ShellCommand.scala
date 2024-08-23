package de.unruh.quickfind
package items

import core.{LeafItem, SVGImage, ScalableImage, SnippetPreviewItem}

import ShellCommand.*

class ShellCommand(command: String, trust: trusted.type, preview: Option[(String,String,String)])
  extends LeafItem, SnippetPreviewItem(preview) {
  override val equalityKey: AnyRef = (command, trust, preview)
  override def title: String = s"Run: $command"
  override def toString: String = s"[Shell $command]"

  override def icon: ScalableImage = ShellCommand.icon
  override def defaultAction(): Unit =
    import sys.process.*
    Seq("bash", "-c", command).run()
}

object ShellCommand {
  /** An object that needs to be passed to [[ShellCommand]] as a _reminder_
   * that a [[ShellCommand]] should only be created from trusted sources.
   * Nothing is actually enforced.
   * */
  object trusted
  private val icon: SVGImage = SVGImage.fromResource("/icons/execute-svgrepo-com.svg")
}