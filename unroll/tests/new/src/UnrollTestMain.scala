package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val cls = classOf[Unrolled]

    assert(scala.util.Try(cls.getConstructor(classOf[String])).isFailure)
    println()
    assert(
      cls.getConstructor(classOf[String], classOf[Int]).newInstance("hello", 2) ==
      "hello2true0"
    )
    assert(
      cls.getConstructor(classOf[String], classOf[Int], classOf[Boolean])
        .newInstance("hello", 2, false) ==
      "hello2false0"
    )
    assert(
      cls.getConstructor(classOf[String], classOf[Int], classOf[Boolean], classOf[Long])
        .newInstance("hello", 2, false, 3) ==
      "hello2false3"
    )

    cls.getMethods.filter(_.getName.contains("foo")).foreach(println)
  }
}














