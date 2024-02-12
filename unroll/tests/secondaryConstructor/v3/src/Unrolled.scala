package unroll

import scala.annotation.unroll

class Unrolled() {
  var foo = ""

  def this(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0) = {
    this()
    foo = s + n + b + l
  }
}







