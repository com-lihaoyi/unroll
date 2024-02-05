package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(Unrolled.foo("cow"))
    println(Unrolled.foo("cow", 2))
    println(Unrolled.foo("cow", 2, false))

    assert(Unrolled.foo("cow").startsWith("cow1true"))
    assert(Unrolled.foo("cow", 2).startsWith("cow2true"))
    assert(Unrolled.foo("cow", 2, false).startsWith("cow2false"))
  }
}














