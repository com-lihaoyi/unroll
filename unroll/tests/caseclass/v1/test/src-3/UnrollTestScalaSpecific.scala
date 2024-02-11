package unroll

import unroll.TestUtils.logAssertStartsWith

object UnrollTestScalaSpecific{
  def test() = {
    val unrolled = Unrolled.fromProduct(
      new Product{
        def canEqual(that: Any) = true
        def productArity = 2
        def productElement(n: Int) = n match{
          case 0 => "hello"
          case 1 => 31337
        }
      }
    )

    logAssertStartsWith(unrolled.foo, "hello31337")
  }
}
