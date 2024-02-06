package unroll

class Unrolled() {
  private[this] var s: String = ""
  private[this] var n: Int = 0
  private[this] var b: Boolean = false
  private[this] var l: Long = 0

  @unroll.Unroll("b")
  def this(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = {
    this()
    this.s = s
    this.n = n
    this.b = b
    this.l = l
  }
  def foo = s + n + b + l
}







