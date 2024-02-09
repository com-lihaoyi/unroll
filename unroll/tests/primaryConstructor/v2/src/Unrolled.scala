package unroll

class Unrolled(s: String, n: Int = 1, @Unroll b: Boolean = true){
  def foo = s + n + b
}
