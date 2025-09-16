package de.unruh.quickfind

object Tmp {
  def main(args: Array[String]): Unit = {
    val re = raw"(x)?(y)".r
    val m = re.findFirstMatchIn("y").get
    println(m)
    println(m.subgroups)
    val List(x, y) = m.subgroups : List[String | Null]
    println(x)
    if (x==null) println("null")
  }
}
