package unroll

case class Unrolled(s: String, n: Int = 1, @Unroll b: Boolean = true, l: Long = 0){
  def foo = s + n + b + l
}
