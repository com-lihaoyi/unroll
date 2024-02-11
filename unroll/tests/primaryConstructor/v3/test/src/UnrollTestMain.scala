package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    UnrollTestPlatformSpecific()
    
    logAssertStartsWith(new Unrolled("cow").foo, "cow1true0")
    logAssertStartsWith(new Unrolled("cow", 2).foo, "cow2true0")
    logAssertStartsWith(new Unrolled("cow", 2, false).foo, "cow2false0")
    logAssertStartsWith(new Unrolled("cow", 2, false, 3).foo, "cow2false3")
  }
}














