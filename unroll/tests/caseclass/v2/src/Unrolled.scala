package unroll

import com.lihaoyi.unroll

case class Unrolled(s: String, n: Int = 1, @unroll b: Boolean = true){
  def foo = s + n + b
}

