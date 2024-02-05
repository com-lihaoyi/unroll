package unroll

class UnrollTestMain{

  def print2(x: Any) = println(x)

  @unroll.Unroll("n")
  def foo(s: String, n: Int = 1) = print2(s + n)

}


object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    new UnrollTestMain().foo("hello", 2)

    var i = 0
    val methods = classOf[UnrollTestMain].getMethods
    while (i < methods.length) {
      if (methods(i).getName.contains("foo")) println(methods(i))
      i += 1
    }
  }
}

