package unroll

class Unrolled @unroll.Unroll("b") (s: String, n: Int = 1, b: Boolean = true){
  def foo = s + n + b
}
