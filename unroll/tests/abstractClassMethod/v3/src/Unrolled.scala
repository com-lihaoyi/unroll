package unroll

abstract class Unrolled{
  def foo(s: String, n: Int = 1, @Unroll b: Boolean = true, l: Long = 0): String
}

object Unrolled extends Unrolled{
  def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l
}
class UnrolledCls extends Unrolled{
  def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l
}
