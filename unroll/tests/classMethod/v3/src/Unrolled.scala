package unroll

import scala.annotation.unroll

class Unrolled{
  def foo(s: String, @unroll n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l
}






