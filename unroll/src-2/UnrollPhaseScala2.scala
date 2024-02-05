package unroll

import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.{Global, Phase}
import tools.nsc.plugins.PluginComponent

class UnrollPhaseScala2(val global: Global) extends PluginComponent with TypingTransformers with Transform {

  import global._

  val runsAfter = List("typer")

  override val runsBefore = List("patmat")

  val phaseName = "unroll"

  override def newTransformer(unit: global.CompilationUnit): global.AstTransformer = {
    new UnrollTransformer(unit)
  }

  class UnrollTransformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {
    def transformClassDef(md: ModuleDef): ModuleDef = {
      val allNewMethods = md.impl.body.collect{ case defdef: DefDef =>
        defdef.symbol.annotations.filter(_.tpe =:= typeOf[unroll.Unroll]).flatMap{ annot =>
          annot.tree.children.tail.map(_.asInstanceOf[NamedArg].rhs) match{
            case Seq(Literal(Constant(s: String))) =>
              val flattenedValueParams = defdef.vparamss.flatten
              val startParamIndex = flattenedValueParams.indexWhere(_.name.toString == s)
              val endParamIndex = flattenedValueParams.size
              assert(startParamIndex != -1)

              for(paramIndex <- Range(startParamIndex, endParamIndex)) yield {
                val forwarderCall = Apply(
                  fun = Select(
                    This(TypeName("UnrolledTestMain")).setType(md.tpe),
                    defdef.name
                  ).setType(defdef.tpe),
                  args =
                    flattenedValueParams.take(paramIndex).map(p => Ident(p.name).setType(p.tpe)) ++
                    Seq(Ident(defdef.name.toString + "$default$" + (paramIndex + 1)))
                ).setType(defdef.symbol.asMethod.returnType)

                val forwarderDef = treeCopy.DefDef(
                  defdef,
                  mods = defdef.mods,
                  name = defdef.name,
                  tparams = defdef.tparams,
                  vparamss = List(
                    flattenedValueParams
                      .take(paramIndex)
                      .map { vp =>
                        treeCopy.ValDef(vp, vp.mods, vp.name, vp.tpt, EmptyTree)
                      }
                  ),
                  tpt = defdef.tpt,
                  rhs = forwarderCall
                )

                println("A")
                val res = forwarderDef
                println("B")
                res
              }
          }
        }
      }

      println("allNewMethods.flatten.size " + allNewMethods.flatten.size)

      treeCopy.ModuleDef(
        md,
        mods = md.mods,
        name = md.name,
        impl = treeCopy.Template(
          md.impl,
          parents = md.impl.parents,
          self = md.impl.self,
          body = md.impl.body ++ allNewMethods.flatten
        )
      )
    }
    override def transform(tree: global.Tree): global.Tree = {
      tree match{
        case d: ModuleDef => super.transform(transformClassDef(d))
        case _ => super.transform(tree)
      }
    }
  }
}

