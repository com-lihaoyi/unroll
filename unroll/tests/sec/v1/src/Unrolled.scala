package unroll

class Unrolled(){
  private[this] var s: String = ""
  private[this] var n: Int = 0
  def this(s: String, n: Int = 1) = {
    this()
    this.s = s
    this.n = n
  }
  def foo = s + n
}
