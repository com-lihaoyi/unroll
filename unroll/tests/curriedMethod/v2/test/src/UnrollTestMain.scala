package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    logAssertStartsWith(new Unrolled().foo("cow")(identity), "cow1true")
    logAssertStartsWith(new Unrolled().foo("cow", 2)(identity), "cow2true")
    logAssertStartsWith(new Unrolled().foo("cow", 2, false)(identity), "cow2false")
  }
}














