package unroll

import com.lihaoyi.unroll

class Unrolled() {
  var foo = ""

  def this(s: String, n: Int = 1, @unroll b: Boolean = true) = {
    this()
    foo = s + n + b
  }
}
