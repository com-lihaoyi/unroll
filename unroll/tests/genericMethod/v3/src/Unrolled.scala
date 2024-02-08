package unroll

class Unrolled{
  @unroll.Unroll("n")
  def foo[T](s: T, n: Int = 1, b: Boolean = true, l: Long = 0) = s.toString + n + b + l
}






