package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val unrolled = new Unrolled{}
    println(unrolled.foo("cow"))
    println(unrolled.foo("cow", 2))

    assert(unrolled.foo("cow").startsWith("cow1"))
    assert(unrolled.foo("cow", 2).startsWith("cow2"))
  }
}














