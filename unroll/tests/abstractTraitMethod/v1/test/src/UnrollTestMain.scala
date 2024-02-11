package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    val unrolled = new UnrolledCls
    logAssertStartsWith(unrolled.foo("cow"), "cow1")
    logAssertStartsWith(unrolled.foo("cow", 2), "cow2")

    logAssertStartsWith(Unrolled.foo("cow"), "cow1")
    logAssertStartsWith(Unrolled.foo("cow", 2), "cow2")
  }
}
