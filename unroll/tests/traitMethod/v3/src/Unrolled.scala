package unroll

import com.lihaoyi.unroll

trait Unrolled{
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true, @unroll l: Long = 0) = s + n + b + l
}

object Unrolled extends Unrolled