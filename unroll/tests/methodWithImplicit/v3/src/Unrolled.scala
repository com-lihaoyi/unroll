package unroll

class Unrolled{
  @unroll.Unroll("n")
  def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0)(implicit f: String => String) = f(s + n + b + l)
}



