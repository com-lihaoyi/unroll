package unroll

import scala.annotation.unroll

class Unrolled(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0){
  def foo = s + n + b + l
}







