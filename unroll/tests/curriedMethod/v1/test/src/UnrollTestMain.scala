package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    logAssertStartsWith(new Unrolled().foo("cow")(identity), "cow")
  }
}














