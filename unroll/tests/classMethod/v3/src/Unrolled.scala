package unroll

class Unrolled{
  def foo(s: String, @Unroll n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l
}






