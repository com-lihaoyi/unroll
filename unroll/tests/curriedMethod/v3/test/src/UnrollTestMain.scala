package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled().foo("cow")(identity))
    println(new Unrolled().foo("cow", 2)(identity))
    println(new Unrolled().foo("cow", 2, false)(identity))
    println(new Unrolled().foo("cow", 2, false, 3)(identity))

    assert(new Unrolled().foo("cow")(identity).startsWith("cow1true0"))
    assert(new Unrolled().foo("cow", 2)(identity).startsWith("cow2true0"))
    assert(new Unrolled().foo("cow", 2, false)(identity).startsWith("cow2false0"))
    assert(new Unrolled().foo("cow", 2, false, 3)(identity).startsWith("cow2false3"))
  }
}














