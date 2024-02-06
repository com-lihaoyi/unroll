package unroll

class Unrolled() {
  var foo = ""

  @unroll.Unroll("b")
  def this(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = {
    this()
    foo = s + n + b + l
  }
}







