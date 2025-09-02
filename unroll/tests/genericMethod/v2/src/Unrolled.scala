package unroll

import com.lihaoyi.unroll

final class Unrolled{
  def foo[T](s: T, @unroll n: Int = 1, b: Boolean = true) = s.toString + n + b
}
