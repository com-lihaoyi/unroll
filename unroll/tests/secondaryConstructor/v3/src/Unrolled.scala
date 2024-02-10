package unroll

class Unrolled() {
  var foo = ""

  def this(s: String, n: Int = 1, @Unroll b: Boolean = true, l: Long = 0) = {
    this()
    foo = s + n + b + l
  }
}







