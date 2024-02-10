package unroll

object TestUtils{
  def logAssertStartsWith[T](t: T, s: String) = {
    println(t)
    // We use .startsWith for our tests for backwards compatibility testing.
    // As "new versions" of the code add more parameters to the class or method,
    // the returned string gets longer, but the prefix remains the same. By using
    // `.startsWith`, we emulate the real-world scenario where the behavior of a
    // class or method changes, but does so in a backwards compatible manner: adding
    // new functionality while still obeying all previous invariants
    assert(t.toString.startsWith(s))
  }
}