package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    logAssertStartsWith(Unrolled.foo("cow"), "cow1")
    logAssertStartsWith(Unrolled.foo("cow", 2), "cow2")
  }
}














