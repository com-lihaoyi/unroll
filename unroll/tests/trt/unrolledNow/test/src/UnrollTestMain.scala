package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled{}.foo("cow"))
    println(new Unrolled{}.foo("cow", 2))
    println(new Unrolled{}.foo("cow", 2, false))
    println(new Unrolled{}.foo("cow", 2, false, 3))

    assert(new Unrolled{}.foo("cow").startsWith("cow1true0"))
    assert(new Unrolled{}.foo("cow", 2).startsWith("cow2true0"))
    assert(new Unrolled{}.foo("cow", 2, false).startsWith("cow2false0"))
    assert(new Unrolled{}.foo("cow", 2, false, 3).startsWith("cow2false3"))
  }
}














