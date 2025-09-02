package unroll

import com.lihaoyi.unroll

final class Unrolled{
  def foo(s: String, @unroll n: Int = 1, b: Boolean = true, @unroll l: Long = 0) = s + n + b + l
}
