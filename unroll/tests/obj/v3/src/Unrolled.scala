package unroll

object Unrolled{

  @unroll.Unroll("b")
  def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l

  def foobar(x: Int, y: String) = 123
  def foobar(x: Int) = 123
  def foobar() = 123
}




























































