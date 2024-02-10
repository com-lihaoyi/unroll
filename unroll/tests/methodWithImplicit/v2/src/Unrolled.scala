package unroll

class Unrolled{
  def foo(s: String, @Unroll n: Int = 1, b: Boolean = true)(implicit f: String => String) = f(s + n + b)
}
