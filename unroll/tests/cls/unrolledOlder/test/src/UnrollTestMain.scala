package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled().foo("cow"))
    println(new Unrolled().foo("cow", 2))

    assert(new Unrolled().foo("cow").startsWith("cow1"))
    assert(new Unrolled().foo("cow", 2).startsWith("cow2"))
  }
}














