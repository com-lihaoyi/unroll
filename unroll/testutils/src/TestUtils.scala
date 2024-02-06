package unroll

object TestUtils{
  def logAssertStartsWith[T](t: T, s: String) = {
    println(t)
    assert(t.toString.startsWith(s))
  }
}