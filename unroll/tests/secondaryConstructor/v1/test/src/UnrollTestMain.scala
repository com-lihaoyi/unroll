package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestMain{
  def main(args: Array[String]): Unit = {
    logAssertStartsWith(new Unrolled("cow").foo, "cow1")
    logAssertStartsWith(new Unrolled("cow", 2).foo, "cow2")
  }
}














