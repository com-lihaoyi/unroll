package unroll

import unroll.TestUtils.logAssertStartsWith


object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val unrolled = new UnrolledCls
    logAssertStartsWith(unrolled.foo("cow"), "cow1true")
    logAssertStartsWith(unrolled.foo("cow", 2), "cow2true")
    logAssertStartsWith(unrolled.foo("cow", 2, false), "cow2false")

    logAssertStartsWith(UnrolledObj.foo("cow"), "cow1true")
    logAssertStartsWith(UnrolledObj.foo("cow", 2), "cow2true")
    logAssertStartsWith(UnrolledObj.foo("cow", 2, false), "cow2false")
  }
}














