package unroll

class Unrolled() {
  var foo = ""

  @unroll.Unroll("b")
  def this(s: String, n: Int = 1, b: Boolean = true) = {
    this()
    foo = s + n + b
  }
}
