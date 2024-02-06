package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val cls = classOf[Unrolled]

    assert(scala.util.Try(cls.getConstructor(classOf[String])).isFailure)
    println()
    assert(
      cls.getConstructor(classOf[String], classOf[Int])
        .newInstance("hello", 2: Integer)
        .asInstanceOf[Unrolled]
        .foo ==
      "hello2true0"
    )
    assert(
      cls.getConstructor(classOf[String], classOf[Int], classOf[Boolean])
        .newInstance("hello", 2: Integer, java.lang.Boolean.FALSE)
        .asInstanceOf[Unrolled]
        .foo ==
      "hello2false0"
    )
    assert(
      cls.getConstructor(classOf[String], classOf[Int], classOf[Boolean], classOf[Long])
        .newInstance("hello", 2: Integer, java.lang.Boolean.FALSE, 3: Integer)
        .asInstanceOf[Unrolled]
        .foo ==
      "hello2false3"
    )

    cls.getConstructors.foreach(println)
  }
}














