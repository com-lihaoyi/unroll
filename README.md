# unroll

Doesn't work yet, use the following commands to test:

```bash
./mill -i -w "unroll[2.13.11].tests[_]"
./mill -i -w "unroll[3.3.1].tests[_]"
```

## Testing

Unroll is tested via the following cases

| version \ testcase | cls (Class Method) | obj (Object Method) | trt (Trait Method) | new (Class Constructor) |
|--------------------|--------------------|---------------------|--------------------|-------------------------|
| v1                 | 0 defaults         | 1 default           | 1 default          | 1 default               |
| v2                 | 2 default          | 2 defaults          | 2 defaults         | 2 defaults              |
| v3                 | 3 defaults         | 3 defaults          | 3 defaults         | 3 defaults              |

`cls` intentionally has different set of default parameters as the others, to try
and provide broader test coverage.

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
