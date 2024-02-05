package unroll


object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    UnrollTest.foo("hello", 2)

    var i = 0
    val methods = classOf[UnrollTestMain.type].getMethods
    while (i < methods.length) {
      if (methods(i).getName.contains("foo")) println(methods(i))
      i += 1
    }
  }
}













