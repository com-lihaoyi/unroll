package unroll


object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val instance = Unrolled
    val cls = classOf[Unrolled.type]

    cls.getMethods.filter(_.getName.contains("foo")).foreach(println)

    assert(scala.util.Try(cls.getMethod("foo", classOf[String])).isFailure)
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













