package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val unrolled = new Unrolled{}
    println(unrolled.foo("cow"))
    println(unrolled.foo("cow", 2))
    println(unrolled.foo("cow", 2, false))
    println(unrolled.foo("cow", 2, false, 3))

    assert(unrolled.foo("cow").startsWith("cow1true0"))
    assert(unrolled.foo("cow", 2).startsWith("cow2true0"))
    assert(unrolled.foo("cow", 2, false).startsWith("cow2false0"))
    assert(unrolled.foo("cow", 2, false, 3).startsWith("cow2false3"))
  }
}














