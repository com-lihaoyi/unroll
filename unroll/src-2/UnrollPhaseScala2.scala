package unroll

import scala.tools.nsc.symtab.Flags
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
    def transformClassDef(md: ImplDef): ImplDef = {
      val allNewMethods = md.impl.body.collect{ case defdef: DefDef =>
        defdef.symbol.annotations.filter(_.tpe =:= typeOf[unroll.Unroll]).flatMap{ annot =>
          annot.tree.children.tail.map(_.asInstanceOf[NamedArg].rhs) match{
            case Seq(Literal(Constant(s: String))) =>
              val flattenedValueParams = defdef.vparamss.flatten
              val startParamIndex = flattenedValueParams.indexWhere(_.name.toString == s)

              val endParamIndex = flattenedValueParams.size
              assert(startParamIndex != -1)

              for(paramIndex <- Range(startParamIndex, endParamIndex)) yield {
                val newSymbol = defdef.symbol.owner.newMethod(defdef.name)

                newSymbol.setInfo(defdef.symbol.tpe)

                val newVParamss = List(
                  flattenedValueParams
                    .take(paramIndex)
                    .map { vd =>
                      val newVdSymbol = newSymbol.newValueParameter(vd.name)
                      newVdSymbol.info = vd.symbol.tpe
                      val newVd = newStrictTreeCopier.ValDef(vd, vd.mods, vd.name, vd.tpt, EmptyTree).setSymbol(newVdSymbol)
                      newVd
                    }
                )

                val forwardedValueParams = flattenedValueParams
                  .take(paramIndex)
                  .zipWithIndex.map{
                    case (p, i) => Ident(p.name).setType(p.tpe).setSymbol(newVParamss(0)(i).symbol)
                  }

                val defaultCalls = {
                  val mangledName = defdef.name.toString + "$default$" + (paramIndex + 1)
                  val defaultMember = md.symbol.tpe.member(TermName(mangledName))
                  Seq(Ident(mangledName).setSymbol(defaultMember).setType(defaultMember.tpe))
                }

                val forwarderCall = Apply(
                  fun = Select(
                    This(defdef.symbol.owner).setType(ThisType(defdef.symbol.owner)).setSymbol(defdef.symbol.owner),
                    defdef.name
                  ).setType(defdef.symbol.tpe)
                    .setSymbol(defdef.symbol),
                  args = forwardedValueParams ++ defaultCalls
                ).setType(defdef.symbol.asMethod.returnType)

                val forwarderDef = treeCopy.DefDef(
                  defdef,
                  mods = defdef.mods,
                  name = defdef.name,
                  tparams = defdef.tparams,
                  vparamss = newVParamss,
                  tpt = defdef.tpt,
                  rhs = forwarderCall
                )
                println()


                newSymbol.setInfo(forwarderDef.symbol.tpe match {
                  case MethodType(params, result) => MethodType(params.take(paramIndex), result)
                })
                forwarderDef.symbol = newSymbol

                println("defdef.mods.flags " + defdef.mods.flags)
                println("forwarderDef.mods.flags " + forwarderDef.mods.flags)
                println("defdef.symbol.flags " + defdef.symbol.flags)
                println("forwarderDef.symbol.flags " + forwarderDef.symbol.flags)
                println("defdef.symbol.flagString " + defdef.symbol.flagString)
                println("forwarderDef.symbol.flagString " + forwarderDef.symbol.flagString)
                println("defdef.symbol.isStatic " + defdef.symbol.isStatic)
                println("forwarderDef.symbol.isStatic " + forwarderDef.symbol.isStatic)
                println("defdef.symbol.owner.isStaticOwner " + defdef.symbol.owner.isStaticOwner)
                println("forwarderDef.symbol.owner.isStaticOwner " + forwarderDef.symbol.owner.isStaticOwner)
                println("defdef.symbol.hasFlag(Flags.STATIC) " + defdef.symbol.hasFlag(Flags.STATIC))
                println("forwarderDef.symbol.hasFlag(Flags.STATIC) " + forwarderDef.symbol.hasFlag(Flags.STATIC))
                forwarderDef
              }
          }
        }
      }

      md match {
        case _: ModuleDef =>

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
        case classDef: ClassDef =>
          treeCopy.ClassDef(
            md,
            mods = md.mods,
            name = md.name,
            tparams = classDef.tparams,
            impl = treeCopy.Template(
              md.impl,
              parents = md.impl.parents,
              self = md.impl.self,
              body = md.impl.body ++ allNewMethods.flatten
            )
          )
        case _ => ???
      }
    }
    override def transform(tree: global.Tree): global.Tree = {
      tree match{
        case d: ModuleDef =>
          d.symbol.tpe.members.foreach(println)
          super.transform(transformClassDef(d))
        case d: ClassDef => super.transform(transformClassDef(d))
        case _ => super.transform(tree)
      }
    }
  }
}

