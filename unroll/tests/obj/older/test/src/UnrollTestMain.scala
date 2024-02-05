package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(Unrolled.foo("cow"))
    println(Unrolled.foo("cow", 2))

    assert(Unrolled.foo("cow").startsWith("cow1"))
    assert(Unrolled.foo("cow", 2).startsWith("cow2"))
  }
}














