package unroll

import com.lihaoyi.unroll

abstract class Unrolled{
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true): String
}

