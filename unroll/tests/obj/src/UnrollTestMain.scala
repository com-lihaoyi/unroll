package unroll


object UnrollTestMain{

  def print2(x: Any) = println(x)

  @unroll.Unroll("n")
  def foo(s: String, n: Int = 1, b: Boolean = true) = s + n + b

  def main(args: Array[String]): Unit = {
    UnrollTestMain.foo("hello", 2)

    var i = 0
    val methods = classOf[UnrollTestMain.type].getMethods
    while (i < methods.length) {
      if (methods(i).getName.contains("foo")) println(methods(i))
      i += 1
    }
  }
}













