package unroll

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    println(new Unrolled("cow").foo)
    println(new Unrolled("cow", 2).foo)
    println(new Unrolled("cow", 2, false).foo)
    println(new Unrolled("cow", 2, false, 3).foo)

    assert(new Unrolled("cow").foo.startsWith("cow1true0"))
    assert(new Unrolled("cow", 2).foo.startsWith("cow2true0"))
    assert(new Unrolled("cow", 2, false).foo.startsWith("cow2false0"))
    assert(new Unrolled("cow", 2, false, 3).foo.startsWith("cow2false3"))
  }
}














