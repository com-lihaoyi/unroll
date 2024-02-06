# unroll

Doesn't work yet, use the following commands to test:

```bash
./mill -i -w "unroll[2.13.11].tests[_]"
./mill -i -w "unroll[3.3.1].tests[_]"
```

## Usage

Unroll provides the `@unroll.Unroll(from: String)` annotation that can be applied
to methods, classes, and constructors. `@Unroll` generates unrolled/telescoping
versions of the method, starting from the parameter specified by `from`, which
are simple forwarders to the primary method or constructor implementation. 

This makes it easy to preserve binary compatibility when adding default parameters
to methods, classes, and constructors. Downstream code compiled against an old
version of your library with fewer parameters would continue to work, calling the
generated forwarders.

### Methods

```scala
object Unrolled{
   @unroll.Unroll("b")
   def foo(s: String, n: Int = 1, b: Boolean = true, l: Long = 0) = s + n + b + l
}
```

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

   def this(s: String, n: Int, b: Boolean) = this(s, n, b, 0)
   def this(s: String, n: Int) = this(s, n, true, 0)
}

```


## Testing

Unroll is tested via the following testcases

1. `cls` (Class Method)
2. `obj` (Object Method)
3. `trt` (Trait Method)
4. `pri` (Primary Constructor

Each of these cases has three versions, `v1` `v2` `v3`, each of which has 
different numbers of default parameters

For each testcase, we have the following tests:

1. `unroll[<scala-version>].tests[<config>]`: Using java reflection to make
   sure the correct methods are generated in version `v3` and are callable with the
   correct output

2. `unroll[<scala-version>].tests[<config>].{v1,v2,v3}.test`: Tests compiled against
   the respective version of a testcase and running against the same version

3. `unroll[<scala-version>].tests[<config>].{v1v2,v2v3,v1v3}.test`: Tests compiled
   an *older* version of a testcase but running against *newer* version. This simulates
   a downstream library compiled against an older version of an upstream library but
   running against a newer version, ensuring there is backwards binary compatibility

4. `unroll[<scala-version>].tests[<config>].{v1,v2,v3}.mimaReportBinaryIssues`: Running
   the Scala [MiMa Migration Manager](https://github.com/lightbend/mima) to check a newer
   version of testcase against an older version for binary compatibility
