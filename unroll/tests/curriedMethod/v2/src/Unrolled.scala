package unroll

import com.lihaoyi.unroll

final class Unrolled{
  def foo(s: String, @unroll n: Int = 1, b: Boolean = true)(f: String => String) = f(s + n + b)
}
