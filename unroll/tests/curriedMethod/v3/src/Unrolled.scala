package unroll

class Unrolled{
  def foo(s: String, @Unroll n: Int = 1, b: Boolean = true, l: Long = 0)(f: String => String) = f(s + n + b + l)
}



