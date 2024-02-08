package unroll

class Unrolled{
  @unroll.Unroll("n")
  def foo[T](s: T, n: Int = 1, b: Boolean = true) = s.toString + n + b
}

