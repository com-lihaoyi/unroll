package unroll

import com.lihaoyi.unroll

final class Unrolled{
  def foo[T](s: T, @unroll n: Int = 1, b: Boolean = true, @unroll l: Long = 0) = s.toString + n + b + l
}
