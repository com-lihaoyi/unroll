package unroll

import com.lihaoyi.unroll

final class Unrolled{
  def foo(f: String => String)(s: String, @unroll n: Int = 1, b: Boolean = true) = f(s + n + b)
}
