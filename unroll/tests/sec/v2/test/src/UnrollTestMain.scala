package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled("cow").foo)
    println(new Unrolled("cow", 2).foo)
    println(new Unrolled("cow", 2, false).foo)

    assert(new Unrolled("cow").foo.startsWith("cow1true"))
    assert(new Unrolled("cow", 2).foo.startsWith("cow2true"))
    assert(new Unrolled("cow", 2, false).foo.startsWith("cow2false"))
  }
}














