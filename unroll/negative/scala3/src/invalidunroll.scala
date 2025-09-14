package unroll

import com.lihaoyi.unroll

class Foo {
  def foo(int: Int, @unroll str: String = "1") = int.toString() + str

  def fooInner(int: Int) = {
    def inner(a: Int, @unroll str: String = "1") = {
      def anEvenMoreInnerMethUnrolled(@unroll int: Int = 1) = ()
      str
    }
    def innerNonUnrollMethod(str: String) = str
  }
}

abstract class FooAbstractClass {
  def foo(s: String, @unroll n: Int = 1): String
}

trait FooTrait(@unroll val param: Int = 1, @unroll val param2: String = "asd") {
  def foo(s: String, @unroll n: Int = 1): String
}

case class FooCaseClass(int: Int, str: String) {
  final def copy(int: Int = int, @unroll str: String = str): FooCaseClass = FooCaseClass(int, str)
}

object FooCaseClass {
  def apply(int: Int, @unroll str: String): FooCaseClass = FooCaseClass(int, str)
}
