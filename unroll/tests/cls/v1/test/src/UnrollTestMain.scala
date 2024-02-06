package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled().foo("cow"))

    assert(new Unrolled().foo("cow").startsWith("cow"))
  }
}














