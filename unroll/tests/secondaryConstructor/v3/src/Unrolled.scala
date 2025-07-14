package unroll

import com.lihaoyi.unroll

class Unrolled() {
  var foo = ""

  def this(s: String, n: Int = 1, @unroll b: Boolean = true, @unroll l: Long = 0) = {
    this()
    foo = s + n + b + l
  }
}







