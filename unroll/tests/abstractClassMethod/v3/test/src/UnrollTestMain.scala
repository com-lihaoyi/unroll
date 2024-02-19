package unroll

import unroll.TestUtils.logAssertStartsWith


object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    UnrollTestPlatformSpecific()

    val unrolled = new UnrolledCls
    logAssertStartsWith(unrolled.foo("cow"), "cow1true0".take(UnrollMisc.expectedLength))
    logAssertStartsWith(unrolled.foo("cow", 2), "cow2true0".take(UnrollMisc.expectedLength))
    logAssertStartsWith(unrolled.foo("cow", 2, false), "cow2fals0".take(UnrollMisc.expectedLength))
    logAssertStartsWith(unrolled.foo("cow", 2, false, 3), "cow2fals3".take(UnrollMisc.expectedLength))

    logAssertStartsWith(UnrolledObj.foo("cow"), "cow1true0".take(UnrollMisc.expectedLength))
    logAssertStartsWith(UnrolledObj.foo("cow", 2), "cow2true0".take(UnrollMisc.expectedLength))
    logAssertStartsWith(UnrolledObj.foo("cow", 2, false), "cow2fals0".take(UnrollMisc.expectedLength))
    logAssertStartsWith(UnrolledObj.foo("cow", 2, false, 3), "cow2fals3".take(UnrollMisc.expectedLength))
  }
}














