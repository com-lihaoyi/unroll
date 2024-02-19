package unroll


object UnrolledObj extends Unrolled {
  def foo(s: String, n: Int = 1, b: Boolean = true) = s + n + b
}

class UnrolledCls extends Unrolled {
  def foo(s: String, n: Int = 1, b: Boolean = true) = s + n + b
}
