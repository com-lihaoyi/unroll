package unroll

abstract class Unrolled{
  def foo(s: String, n: Int = 1): String
}

object Unrolled extends Unrolled{
  def foo(s: String, n: Int = 1) = s + n
}

class UnrolledCls extends Unrolled{
  def foo(s: String, n: Int = 1) = s + n
}
