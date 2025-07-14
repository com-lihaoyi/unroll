package unroll

import com.lihaoyi.unroll

class Unrolled{
  def foo(s: String, @unroll n: Int = 1, b: Boolean = true)(f: String => String) = f(s + n + b)
}
