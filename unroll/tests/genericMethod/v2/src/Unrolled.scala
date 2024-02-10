package unroll

class Unrolled{
  def foo[T](s: T, @Unroll n: Int = 1, b: Boolean = true) = s.toString + n + b
}

