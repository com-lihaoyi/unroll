package unroll

class UnrollTestMain{

  def print2(x: Any) = println(x)

  @unroll.Unroll("b")
  def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l

}


object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val instance = new UnrollTestMain()
    val cls = classOf[UnrollTestMain]

    assert(scala.util.Try(cls.getMethod("foo", classOf[String])).isFailure)
    println()
    assert(
      cls.getMethod("foo", classOf[String], classOf[Int]).invoke(instance, "hello", 2) ==
      "hello2true0"
    )
    assert(
      cls.getMethod("foo", classOf[String], classOf[Int], classOf[Boolean])
        .invoke(instance, "hello", 2, false) ==
      "hello2false0"
    )
    assert(
      cls.getMethod("foo", classOf[String], classOf[Int], classOf[Boolean], classOf[Long])
        .invoke(instance, "hello", 2, false, 3) ==
      "hello2false3"
    )
  }
}













