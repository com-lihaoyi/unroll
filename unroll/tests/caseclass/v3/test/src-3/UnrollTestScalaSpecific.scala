package unroll
import unroll.TestUtils.logAssertStartsWith
object UnrollTestScalaSpecific{
  def apply() = {
    val unrolled = Unrolled.fromProduct(
      new Product {
        def canEqual(that: Any) = true
        def productArity = 4
        def productElement(n: Int) = n match {
          case 0 => "hello"
          case 1 => 31337
          case 2 => false
          case 3 => 12345L
        }
      }
    )

    logAssertStartsWith(unrolled.foo, "hello31337false12345")
  }
}