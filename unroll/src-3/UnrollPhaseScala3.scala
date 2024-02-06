package unroll

import dotty.tools.dotc.*
import plugins.*
import core.*
import Contexts.*
import Symbols.*
import Flags.*
import SymDenotations.*
import Decorators.*
import ast.Trees.*
import ast.tpd
import StdNames.nme
import Names.*
import Constants.Constant
import dotty.tools.dotc.core.NameKinds.DefaultGetterName
import dotty.tools.dotc.core.Types.{MethodType, NamedType}
import dotty.tools.dotc.core.Symbols

import scala.language.implicitConversions

class UnrollPhaseScala3() extends PluginPhase {
  import tpd._

  val phaseName = "unroll"

  override val runsAfter = Set(transform.Pickler.name)

  override def transformTemplate(tmpl: tpd.Template)(using Context): tpd.Tree = {
    val newMethods = (tmpl.body ++ Seq(tmpl.constr)).collect{
      case defdef: DefDef =>
        import dotty.tools.dotc.core.NameOps.isConstructorName
        val allParams = defdef.paramss.asInstanceOf[List[List[ValDef]]].flatten

        val annotated = if (defdef.symbol.isPrimaryConstructor) defdef.symbol.owner else defdef.symbol

        for(annot <- annotated.annotations.find(_.symbol.fullName.toString == "unroll.Unroll")) yield {
          val Some(Literal(Constant(argName: String))) = annot.argument(0)

          val argIndex = allParams.indexWhere(_.name.toString == argName)

          val prevMethodType = defdef.symbol.info.asInstanceOf[MethodType]
          for(n <- Range(argIndex, allParams.size)) yield{
            val truncatedMethodType = MethodType(
              prevMethodType.paramInfos.take(n),
              prevMethodType.resType
            )

            val newSymbol = Symbols.newSymbol(defdef.symbol.owner, defdef.name, defdef.symbol.flags, truncatedMethodType)

            val paramss = allParams.take(n).map { p =>
              implicitly[Context].typeAssigner.assignType(
                cpy.ValDef(p)(defdef.name, p.tpt, p.rhs),
                Symbols.newSymbol(newSymbol, p.name, p.symbol.flags, p.symbol.info)
              )
            }

            val applyTree = Apply(
              This(defdef.symbol.owner.asClass).select(defdef.symbol),
              paramss.map(p => ref(p.symbol)) ++
                Range(n, allParams.size).map(n2 =>
                  if (defdef.symbol.isConstructor){
                    ref(defdef.symbol.owner.companionModule)
                      .select(DefaultGetterName(defdef.name, n2))
                  }else{
                    This(defdef.symbol.owner.asClass)
                      .select(DefaultGetterName(defdef.name, n2))
                  }
                )
            )

            val newDefDef = implicitly[Context].typeAssigner.assignType(
              cpy.DefDef(defdef)(
                name = newSymbol.name,
                paramss = List(paramss),
                tpt = defdef.tpt,
                rhs =
                  if (defdef.symbol.isConstructor){
                    Block(
                      List(applyTree),
                      Literal(Constant(()))
                    )
                  }else applyTree
              ),
              newSymbol
            )

            newDefDef
          }
        }
    }

    super.transformTemplate(
      cpy.Template(tmpl)(
        tmpl.constr,
        tmpl.parents,
        tmpl.derived,
        tmpl.self,
        tmpl.body ++ newMethods.flatten.flatten
      )
    )
  }
}
