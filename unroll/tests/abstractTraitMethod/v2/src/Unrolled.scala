package unroll

import scala.annotation.unroll

trait Unrolled{
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true): String
}

object Unrolled extends Unrolled{
  def foo(s: String, n: Int = 1, b: Boolean = true) = s + n + b
}

class UnrolledCls extends Unrolled{
  def foo(s: String, n: Int = 1, b: Boolean = true) = s + n + b
}