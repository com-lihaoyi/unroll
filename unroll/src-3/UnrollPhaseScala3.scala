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
    def copyParam(p: ValDef, parent: Symbol) = {
      implicitly[Context].typeAssigner.assignType(
        cpy.ValDef(p)(p.name, p.tpt, p.rhs),
        Symbols.newSymbol(parent, p.name, p.symbol.flags, p.symbol.info)
      )
    }

    def potentialDefDefs = (tmpl.body ++ Seq(tmpl.constr)).collect{ case defdef: DefDef if defdef.paramss.nonEmpty => defdef }

    val newMethods = for(defdef <- potentialDefDefs) yield {
      import dotty.tools.dotc.core.NameOps.isConstructorName
      val firstParams :: restParams = defdef.paramss.asInstanceOf[List[List[ValDef]]]

      val annotated = if (defdef.symbol.isPrimaryConstructor) defdef.symbol.owner else defdef.symbol

      for (annot <- annotated.annotations.find(_.symbol.fullName.toString == "unroll.Unroll")) yield {
        val Some(Literal(Constant(argName: String))) = annot.argument(0)

        val argIndex = firstParams.indexWhere(_.name.toString == argName)

        val prevMethodType = defdef.symbol.info.asInstanceOf[MethodType]
        for (n <- Range(argIndex, firstParams.size)) yield {
          val truncatedMethodType = MethodType(
            prevMethodType.paramInfos.take(n),
            prevMethodType.resType
          )

          val forwarderDefSymbol = Symbols.newSymbol(
            defdef.symbol.owner,
            defdef.name,
            defdef.symbol.flags,
            truncatedMethodType
          )

          val newFirstParamss = firstParams.take(n).map(copyParam(_, forwarderDefSymbol))
          val newRestParamss = restParams.map(_.map(copyParam(_, forwarderDefSymbol)))

          val defaultCalls = for(n <- Range(n, firstParams.size)) yield {
            if (defdef.symbol.isConstructor) {
              ref(defdef.symbol.owner.companionModule)
                .select(DefaultGetterName(defdef.name, n))
            } else {
              This(defdef.symbol.owner.asClass)
                .select(DefaultGetterName(defdef.name, n))
            }
          }
          
          val allNewParamTrees = 
            List(newFirstParamss.map(p => ref(p.symbol)) ++ defaultCalls) ++
            newRestParamss.map(_.map(p => ref(p.symbol)))
          
          var applyTree: Tree = This(defdef.symbol.owner.asClass).select(defdef.symbol)

          for (newParams <- allNewParamTrees) applyTree = Apply(applyTree, newParams)

          val newDefDef = implicitly[Context].typeAssigner.assignType(
            cpy.DefDef(defdef)(
              name = forwarderDefSymbol.name,
              paramss = List(newFirstParamss) ++ newRestParamss,
              tpt = defdef.tpt,
              rhs =
                if (!defdef.symbol.isConstructor) applyTree
                else Block(List(applyTree), Literal(Constant(())))
            ),
            forwarderDefSymbol
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
