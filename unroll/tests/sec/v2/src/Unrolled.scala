package unroll

class Unrolled() {
  private[this] var s: String = ""
  private[this] var n: Int = 0
  private[this] var b: Boolean = false

  @unroll.Unroll("b")
  def this(s: String, n: Int = 1, b: Boolean = true) = {
    this()
    this.s = s
    this.n = n
    this.b = b
  }
  def foo = s + n + b
}
