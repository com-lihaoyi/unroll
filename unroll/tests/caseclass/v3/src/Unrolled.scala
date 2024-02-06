package unroll

@unroll.Unroll("b")
case class Unrolled(s: String, n: Int = 1, b: Boolean = true, l: Long = 0){
  def foo = s + n + b + l
}
