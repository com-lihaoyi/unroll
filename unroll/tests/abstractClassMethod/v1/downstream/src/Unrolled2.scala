package unroll

object UnrolledObj extends Unrolled {
  def foo(s: String, n: Int = 1) = s + n
}

class UnrolledCls extends Unrolled {
  def foo(s: String, n: Int = 1) = s + n
}