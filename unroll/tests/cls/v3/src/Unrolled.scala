package unroll

class Unrolled{
  @unroll.Unroll("b")
  def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l
}



