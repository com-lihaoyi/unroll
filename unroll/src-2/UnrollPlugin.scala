package unroll

import tools.nsc.Global

class UnrollPlugin(val global: Global)
  extends tools.nsc.plugins.Plugin {
  println("new UnrollPlugin")
  val name = "unroll"

  val description = "Plugin to unroll default methods for binary compatibility"

  val components = List(new UnrollPhase(this.global))
}