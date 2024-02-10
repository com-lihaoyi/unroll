package unroll

trait Unrolled{
  def foo(s: String, n: Int = 1, @Unroll b: Boolean = true, l: Long = 0) = s + n + b + l
}


object Unrolled extends Unrolled