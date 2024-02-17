package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    UnrollTestPlatformSpecific()
    
    logAssertStartsWith(new Unrolled().foo(identity)("cow"), "cow1true0")
    logAssertStartsWith(new Unrolled().foo(identity)("cow", 2), "cow2true0")
    logAssertStartsWith(new Unrolled().foo(identity)("cow", 2, false), "cow2false0")
    logAssertStartsWith(new Unrolled().foo(identity)("cow", 2, false, 3), "cow2false3")
  }
}














