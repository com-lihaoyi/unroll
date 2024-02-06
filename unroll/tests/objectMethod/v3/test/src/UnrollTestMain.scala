package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(Unrolled.foo("cow"))
    println(Unrolled.foo("cow", 2))
    println(Unrolled.foo("cow", 2, false))
    println(Unrolled.foo("cow", 2, false, 3))

    assert(Unrolled.foo("cow").startsWith("cow1true0"))
    assert(Unrolled.foo("cow", 2).startsWith("cow2true0"))
    assert(Unrolled.foo("cow", 2, false).startsWith("cow2false0"))
    assert(Unrolled.foo("cow", 2, false, 3).startsWith("cow2false3"))
  }
}














