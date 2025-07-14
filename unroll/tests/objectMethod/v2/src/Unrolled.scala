package unroll

import com.lihaoyi.unroll

object Unrolled{
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true) = s + n + b
}



