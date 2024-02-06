# unroll


Unroll provides the `@unroll.Unroll(from: String)` annotation that can be applied
to methods, classes, and constructors. `@Unroll` generates unrolled/telescoping
versions of the method, starting from the parameter specified by `from`, which
are simple forwarders to the primary method or constructor implementation. 

This makes it easy to preserve binary compatibility when adding default parameters
to methods, classes, and constructors. Downstream code compiled against an old
version of your library with fewer parameters would continue to work, calling the
generated forwarders.

## Usage

### Methods

```scala
object Unrolled{
   @unroll.Unroll("b")
   def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l
}
```

Unrolls to:

```scala
object Unrolled{
   @unroll.Unroll("b")
   def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l

   def foo(s: String, n: Int, b: Boolean) = foo(s, n, b, 0)
   def foo(s: String, n: Int) = foo(s, n, true, 0)
}
````
### Classes

```scala
@unroll.Unroll("b")
class Unrolled(s: String, n: Int = 1, b: Boolean = true, l: Long = 0){
   def foo = s + n + b + l
}
```

Unrolls to:

```scala
@unroll.Unroll("b")
class Unrolled(s: String, n: Int = 1, b: Boolean = true, l: Long = 0){
   def foo = s + n + b + l

   def this(s: String, n: Int, b: Boolean) = this(s, n, b, 0)
   def this(s: String, n: Int) = this(s, n, true, 0)
}
```

### Constructors

```scala
class Unrolled() {
   var foo = ""

   @unroll.Unroll("b")
   def this(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = {
      this()
      foo = s + n + b + l
   }
}
```

Unrolls to:

```scala
class Unrolled() {
   var foo = ""

   @unroll.Unroll("b")
   def this(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = {
      this()
      foo = s + n + b + l
   }

   def this(s: String, n: Int, b: Boolean) = this(s, n, b, 0)
   def this(s: String, n: Int) = this(s, n, true, 0)
}
```

### Case Classes

```scala
@unroll.Unroll("b")
case class Unrolled(s: String, n: Int = 1, b: Boolean = true){
  def foo = s + n + b
}
```

Unrolls to:

```scala
@unroll.Unroll("b")
case class Unrolled(s: String, n: Int = 1, b: Boolean = true, l: Long = 0L){
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

`unapply` is not a binary compatibility issue in Scala 3 due to 
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
