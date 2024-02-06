package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled("cow").foo)
    println(new Unrolled("cow", 2).foo)

    assert(new Unrolled("cow").foo.startsWith("cow1"))
    assert(new Unrolled("cow", 2).foo.startsWith("cow2"))
  }
}














