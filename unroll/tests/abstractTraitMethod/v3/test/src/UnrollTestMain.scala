package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    UnrollTestPlatformSpecific()
    
    val unrolled = new UnrolledCls
    logAssertStartsWith(unrolled.foo("cow"), "cow1true0")
    logAssertStartsWith(unrolled.foo("cow", 2), "cow2true0")
    logAssertStartsWith(unrolled.foo("cow", 2, false), "cow2false0")
    logAssertStartsWith(unrolled.foo("cow", 2, false, 3), "cow2false3")

    logAssertStartsWith(Unrolled.foo("cow"), "cow1true0")
    logAssertStartsWith(Unrolled.foo("cow", 2), "cow2true0")
    logAssertStartsWith(Unrolled.foo("cow", 2, false), "cow2false0")
    logAssertStartsWith(Unrolled.foo("cow", 2, false, 3), "cow2false3")
  }
}














