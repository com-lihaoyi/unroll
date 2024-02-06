package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled().foo("cow")(identity))
    println(new Unrolled().foo("cow", 2)(identity))
    println(new Unrolled().foo("cow", 2, false)(identity))

    assert(new Unrolled().foo("cow")(identity).startsWith("cow1true"))
    assert(new Unrolled().foo("cow", 2)(identity).startsWith("cow2true"))
    assert(new Unrolled().foo("cow", 2, false)(identity).startsWith("cow2false"))
  }
}














