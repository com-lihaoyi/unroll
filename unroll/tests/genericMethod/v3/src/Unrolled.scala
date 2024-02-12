package unroll

import scala.annotation.unroll

class Unrolled{
  def foo[T](s: T, @unroll n: Int = 1, b: Boolean = true, l: Long = 0) = s.toString + n + b + l
}






