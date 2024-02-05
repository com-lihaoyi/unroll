package unroll

object UnrollTestMain{


  def print2(x: Any) = println(x)

  @unroll.Unroll("n")
  def foo(s: String, n: Int = 1) = print2(s * n)
  def main(args: Array[String]): Unit = {
    foo("hello", 2)

    getClass.getMethods.toList.filter(_.getName.contains("foo")).foreach(println)
  }
}




