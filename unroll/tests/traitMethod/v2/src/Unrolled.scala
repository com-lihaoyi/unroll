package unroll

trait Unrolled{
  def foo(s: String, n: Int = 1, @Unroll b: Boolean = true) = s + n + b
}

object Unrolled extends Unrolled