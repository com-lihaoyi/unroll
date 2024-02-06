package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled().foo("cow")(identity))

    assert(new Unrolled().foo("cow")(identity).startsWith("cow"))
  }
}














