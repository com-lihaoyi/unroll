package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    logAssertStartsWith(new Unrolled().foo("cow"), "cow1true")
    logAssertStartsWith(new Unrolled().foo("cow", 2), "cow2true")
    logAssertStartsWith(new Unrolled().foo("cow", 2, false), "cow2false")
  }
}














