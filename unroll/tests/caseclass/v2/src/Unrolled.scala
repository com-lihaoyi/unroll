package unroll

@unroll.Unroll("b")
case class Unrolled(s: String, n: Int = 1, b: Boolean = true){
  def foo = s + n + b
}










