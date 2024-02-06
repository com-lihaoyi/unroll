package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val unrolled = new Unrolled{}
    println(unrolled.foo("cow"))
    println(unrolled.foo("cow", 2))
    println(unrolled.foo("cow", 2, false))

    assert(unrolled.foo("cow").startsWith("cow1true"))
    assert(unrolled.foo("cow", 2).startsWith("cow2true"))
    assert(unrolled.foo("cow", 2, false).startsWith("cow2false"))
  }
}














