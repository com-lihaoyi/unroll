package unroll

import com.lihaoyi.unroll

case class Unrolled(s: String, n: Int = 1, @unroll b: Boolean = true, @unroll  l: Long = 0){
  def foo = s + n + b + l
}



