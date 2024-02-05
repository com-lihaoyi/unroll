package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled{}.foo("cow"))
    println(new Unrolled{}.foo("cow", 2))
    println(new Unrolled{}.foo("cow", 2, false))

    assert(new Unrolled {}.foo("cow").startsWith("cow1true"))
    assert(new Unrolled {}.foo("cow", 2).startsWith("cow2true"))
    assert(new Unrolled {}.foo("cow", 2, false).startsWith("cow2false"))
  }
}














