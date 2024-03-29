package unroll
import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    UnrollTestScalaSpecific()
    UnrollTestPlatformSpecific()
    
    logAssertStartsWith(new Unrolled("cow").foo, "cow1true0")
    logAssertStartsWith(new Unrolled("cow", 2).foo, "cow2true0")
    logAssertStartsWith(new Unrolled("cow", 2, false).foo, "cow2false0")
    logAssertStartsWith(new Unrolled("cow", 2, false, 9L).foo, "cow2false9")

    logAssertStartsWith(Unrolled("cow").foo, "cow1true0")
    logAssertStartsWith(Unrolled("cow", 2).foo, "cow2true0")
    logAssertStartsWith(Unrolled("cow", 2, false).foo, "cow2false0")
    logAssertStartsWith(Unrolled("cow", 2, false, 9L).foo, "cow2false9")

    val unrolled = Unrolled("cow")

    logAssertStartsWith(unrolled.copy(s = "cow").foo, "cow1true0")
    logAssertStartsWith(unrolled.copy(s = "cow", n = 2).foo, "cow2true0")
    logAssertStartsWith(unrolled.copy(s = "cow", n = 2, b = false).foo, "cow2false0")
    logAssertStartsWith(unrolled.copy(s = "cow", n = 2, b = false, l = 9L).foo, "cow2false9")

    val Unrolled(s, n, b, l) = unrolled

    assert(s == "cow")
    assert(n == 1)
    assert(b == true)
    assert(l == 0L)

    
  }
}














