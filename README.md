# @unroll


Unroll provides an experimental `@unroll` annotation that can be applied to methods, classes, 
and constructors. `@unroll` generates unrolled/telescoping versions of the method, starting
from the annotated parameter, which are simple forwarders to the primary method or 
constructor implementation. This allows you to maintain binary compatibility when adding
a new default parameter, without the boilerplate of manually defining forwarder methods.

See the following PRs that demonstrate the usage of `@unroll` and the binary-compatibility
boilerplate that can be saved:

- https://github.com/com-lihaoyi/mainargs/pull/113/files
- https://github.com/com-lihaoyi/mill/pull/3008/files
- https://github.com/com-lihaoyi/upickle/pull/555/files

In the past, evolving code in Scala while maintaining binary compatibility was a pain.
You couldn't use default parameters, you couldn't use case classes. Many people fell
back to Java-style builder patterns and factories with `.withFoo` everywhere to maintain binary
compatibility. Or you would tediously define tons of binary compatibility stub methods
that just copy-paste the original signature and forward the call to the new implementation.

In effect, you often gave up everything that made Scala nice to read and write, because
the alternative was worse: every time you added a new parameter to a method, even though
it has a default value, all your users would have to recompile all their code. And all 
*their* users would need to re-compile all their code, transitively. And so library
maintainers would suffer so their users could have a smooth upgrading experience.

With `@unroll`, none of this is a problem anymore. You can add new parameters
where-ever you like: method `def`s, `class`es, `case class`es, etc. As long as the
new parameter has a default value, you can `@unroll` it to generate the binary-compatibility
stub forwarder method automatically. Happy library maintainers, happy users, everyone is happy!

See this original discussion for more context:

* https://contributors.scala-lang.org/t/can-we-make-adding-a-parameter-with-a-default-value-binary-compatible

## Usage

### Methods

```scala
import com.lihaoyi.unroll

object Unrolled{
   def foo(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0) = s + n + b + l
}
```

Unrolls to:

```scala
import com.lihaoyi.unroll

object Unrolled{
   def foo(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0) = s + n + b + l

   def foo(s: String, n: Int, b: Boolean) = foo(s, n, b, 0)
   def foo(s: String, n: Int) = foo(s, n, true, 0)
}
````
### Classes

```scala
import com.lihaoyi.unroll

class Unrolled(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0){
   def foo = s + n + b + l
}
```

Unrolls to:

```scala
import com.lihaoyi.unroll

class Unrolled(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0){
   def foo = s + n + b + l

   def this(s: String, n: Int, b: Boolean) = this(s, n, b, 0)
   def this(s: String, n: Int) = this(s, n, true, 0)
}
```

### Constructors

```scala
import com.lihaoyi.unroll

class Unrolled() {
   var foo = ""

   def this(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0) = {
      this()
      foo = s + n + b + l
   }
}
```

Unrolls to:

```scala
import com.lihaoyi.unroll

class Unrolled() {
   var foo = ""

   def this(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0) = {
      this()
      foo = s + n + b + l
   }

   def this(s: String, n: Int, b: Boolean) = this(s, n, b, 0)
   def this(s: String, n: Int) = this(s, n, true, 0)
}
```

### Case Classes

```scala
import com.lihaoyi.unroll

case class Unrolled(s: String, n: Int = 1, @unroll b: Boolean = true){
  def foo = s + n + b
}
```

Unrolls to:

```scala
import com.lihaoyi.unroll

case class Unrolled(s: String, n: Int = 1, @unroll b: Boolean = true, l: Long = 0L){
   def this(s: String, n: Int) = this(s, n, true, 0L)
   def this(s: String, n: Int, b: Boolean) = this(s, n, b, 0L)
   
   def copy(s: String, n: Int) = copy(s, n, true, 0L)
   def copy(s: String, n: Int, b: Boolean) = copy(s, n, b, 0L)
   
   def foo = s + n + b
}
object Unrolled{
   def apply(s: String, n: Int) = apply(s, n, true, 0L)
   def apply(s: String, n: Int, b: Boolean) = apply(s, n, b, , 0L)
}
```

### Abstract Methods

```scala
import com.lihaoyi.unroll

trait Unrolled{
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true): String
}

object Unrolled extends Unrolled{
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true) = s + n + b
}
```

Unrolls to:

```scala
trait Unrolled{
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true): String = foo(s, n)
  def foo(s: String, n: Int = 1): String
}

object Unrolled extends Unrolled{
  def foo(s: String, n: Int = 1, @unroll b: Boolean = true) = s + n + b
  def foo(s: String, n: Int = 1) = foo(s, n, true)
}
```

Note that only the `@unroll`ed abstract methods generate forwarders in the opposite direction
as the `@unroll`ed concrete methods. This is necessary due to the fact that downstream code does
not know which version of `def foo` contains the implementation, and this "two way" forwarding
ensures that regardless of what signature of `def foo` gets called the forwarders eventually
forwarder to the actual implementation.

## Limitations

1. Only the first parameter list of multi-parameter list methods (i.e. curried or taking
   implicits) can be unrolled. This is an implementation restriction that may be lifted 
   with a bit of work

2. As unrolling generates synthetic forwarder methods for binary compatibility, it is 
   possible for them to collide if your unrolled method has manually-defined overloads

3. Unrolled case classes are only fully binary compatible in Scala 3, though they are
   _almost_ binary compatible in Scala 2. Direct calls to `unapply` are binary incompatible,
   but most common pattern matching of `case class`es goes through a different code path
   that _is_ binary compatible. In practice this should be sufficient for 99% of use cases,
   but it does means that it is possible for code written as below to fail in Scala 2
   if a new unrolled parameter is added to the case class `Unrolled`.

```scala
def foo(t: (String, Int)) = println(t)
Unrolled.unapply(unrolled).map(foo)
```

`unapply` is not a binary compatibility issue in Scala 3, even when called directly, due to 
[Option-less Pattern Matching](https://docs.scala-lang.org/scala3/reference/changed-features/pattern-matching.html)

## Testing

Unroll is tested via a range of test-cases: `classMethod`, `objectMethod`, etc. These
are organized in `build.sc`, to take advantage of the build system's ability to wire up
compilation and classpaths 

Each of these cases has three versions, `v1` `v2` `v3`, each of which has 
different numbers of default parameters

For each test-case, we have the following tests:

1. `unroll[<scala-version>].tests[<test-case>]`: Using java reflection to make
   sure the correct methods are generated in version `v3` and are callable with the
   correct output

2. `unroll[<scala-version>].tests[<test-case>].{v1,v2,v3}.test`: Tests compiled against
   the respective version of a test-case and running against the same version

3. `unroll[<scala-version>].tests[<test-case>].{v1v2,v2v3,v1v3}.test`: Tests compiled
   an *older* version of a test-case but running against *newer* version. This simulates
   a downstream library compiled against an older version of an upstream library but
   running against a newer version, ensuring there is backwards binary compatibility

4. `unroll[<scala-version>].tests[<test-case>].{v1,v2,v3}.mimaReportBinaryIssues`: Running
   the Scala [MiMa Migration Manager](https://github.com/lightbend/mima) to check a newer
   version of test-case against an older version for binary compatibility

You can also run the following command to run all tests:

```bash
./mill -i -w "unroll[_].tests.__.run"         
```

This can be useful as a final sanity check, even though you usually want to run
a subset of the tests specific to the `scala-version` and `test-case` you are 
interested in.

## Upstream

```scala
trait Upstream{ // v1
   def foo(s: String, n: Int = 1)
}
```
```scala
trait Upstream{ // v2
   // source
   def foo(s: String, n: Int = 1, @unroll b: Boolean = true)
   
   // generated
   def foo(s: String, n: Int = 1, b: Boolean = true) = foo(s, n)
   def foo(s: String, n: Int)
}
```

```scala
trait Upstream{ // v3
   // source
   def foo(s: String, n: Int = 1, @unroll b: Boolean = true, @unroll l: Long = 0)
   
   // generated
   def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = fooDown(s, n, b)
   def foo(s: String, n: Int, b: Boolean) = foo(s, n)
   def foo(s: String, n: Int)
}
```

## Downstream

```scala
trait Downstream extends Upstream{ // v1 
   final def foo(s: String, n: Int = 1) = println(s + n)
}
```
```scala
trait Downstream extends Upstream{ // v2
   // source
   def foo(s: String, n: Int = 1, @unroll b: Boolean = true) = println(s + n + b)
   
   // generated
   def foo(s: String, n: Int = 1, b: Boolean = true) = println(s + n + b)
   def foo(s: String, n: Int) = foo(s, n, true)
}
```

```scala
trait Downstream extends Upstream{ // v3
   // source
   def foo(s: String, n: Int = 1, @unroll b: Boolean = true, @unroll l: Long = 0) = println(s + n + b + l)
   
   // generated
   def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = println(s + n + b + l)
   def foo(s: String, n: Int, b: Boolean) = foo(s, n, b, 0)
   def foo(s: String, n: Int) = foo(s, n, true, 0)
}
```