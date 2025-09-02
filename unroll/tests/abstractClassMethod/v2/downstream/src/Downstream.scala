package unroll

import com.lihaoyi.unroll

object UnrolledObj extends Unrolled {
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true) = s + n + b.toString.take(4)
}

final class UnrolledCls extends Unrolled {
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true) = s + n + b.toString.take(4)
}

object UnrollMisc{
  def expectedLength = 6
}
