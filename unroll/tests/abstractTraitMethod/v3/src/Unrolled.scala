package unroll

import com.lihaoyi.unroll

trait Unrolled{
  def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0): String
}
