package de.unruh.quickfind
package apps.calendar

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.{Argument, RuntimeType, SuperCall, This}
import net.bytebuddy.implementation.bytecode.assign.Assigner
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.matcher.ElementMatchers.{isConstructor, named, takesArguments}

import java.awt.datatransfer.DataFlavor
import java.awt.dnd.{DnDConstants, DropTarget, DropTargetAdapter, DropTargetDropEvent}
import java.io.File
import java.lang.reflect.{Field, Modifier}
import java.net.{URI, URISyntaxException}
import javax.swing.{JFrame, JPanel, WindowConstants}
import scala.annotation.internal.AnnotationDefault
import scala.annotation.static
import scala.io.Source
import scala.jdk.CollectionConverters.given

class FixDnD {

}

/** Fixed the problem that XDataTransferer.dragQueryURIs fails if URLs contain [ or ].
 * Thunderbird generates such */
object FixDnD {
  private val patchInstalled = false
  private val parserClass = Class.forName("java.net.URI$Parser")
  private val inputField = parserClass.getDeclaredField("input")

/*
  def installPatch(): Unit = if (!patchInstalled) synchronized {
    System.setProperty("net.bytebuddy.experimental", "true")
    if (!patchInstalled) {
      ByteBuddyAgent.install()

      println(parserClass)
      new ByteBuddy()
//        .redefine(parserClass)
          .redefine(classOf[URI])
//        .method(ElementMatchers.named("parse")
//          .and(ElementMatchers.takesArguments(TypeDescription.ForLoadedType(classOf[Boolean]).asUnboxed())))
//        .intercept(MethodDelegation.to(classOf[FixDnD]))
//          .intercept(Advice.to(classOf[FixDnD2]))
//        .visit(Advice.to(classOf[FixDndJava]).on(named("parse")))
        .visit(Advice.to(classOf[FixDndJava]).on(isConstructor.and(takesArguments(classOf[String]))))
        .make()
        .load(
          ClassLoader.getSystemClassLoader,
          ClassReloadingStrategy.fromInstalledAgent()
        )


    }
  }
*/

/*  @static
  @Advice.OnMethodEnter
  def beforeParse(@Advice.This thiz: AnyRef): Unit = {
    System.out.println("URI.parse")
    System.out.println(thiz.getClass)
    val inputField = thiz.getClass.getDeclaredField("input")
    val input = inputField.get(thiz).asInstanceOf[String]
    if (input.contains("[") || input.contains("]")) {
      // TODO static
//      val modifiersField = classOf[Field].getDeclaredField("modifiers")
//      modifiersField.setAccessible(true)
//      modifiersField.setInt(inputField, inputField.getModifiers & ~Modifier.FINAL)
      val fixedInput = input.replace("[", "%5B").replace("]", "%5D")
      System.out.println(fixedInput)
      inputField.set(thiz, fixedInput)
    }
  }*/

  def main(args: Array[String]): Unit = {
    println(new URI("file://hello"))
    FixDndJava.installPatch()

    println(new URI("file://hel[lo]"))
//    return

    val frame = new JFrame("Drop File Here")
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.setSize(400, 300)

    val panel = new JPanel()

    new DropTarget(panel, new DropTargetAdapter {
      override def drop(dtde: DropTargetDropEvent): Unit = {
        dtde.acceptDrop(DnDConstants.ACTION_COPY)
        val files = dtde.getTransferable
          .getTransferData(DataFlavor.javaFileListFlavor)
          .asInstanceOf[java.util.List[File]]
          .asScala

        files.foreach { file =>
          println(Source.fromFile(file).mkString)
        }

        dtde.dropComplete(true)
      }
    })

    frame.add(panel)
    frame.setVisible(true)
  }
}
