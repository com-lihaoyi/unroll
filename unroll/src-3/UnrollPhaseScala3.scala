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
import dotty.tools.dotc.core.Types.NamedType

import scala.language.implicitConversions

class UnrollPhaseScala3() extends PluginPhase {
  import tpd._

  val phaseName = "unroll"

  override val runsAfter = Set(transform.Pickler.name)

  override def transformTemplate(tmpl: tpd.Template)(using Context): tpd.Tree = {
    val newMethods = tmpl.body.collect{
      case defdef: DefDef =>
        val allParams = defdef.paramss.asInstanceOf[List[List[ValDef]]].flatten
        for(annot <- defdef.symbol.annotations.find(_.symbol.fullName.toString == "unroll.Unroll")) yield {
          val Some(Literal(Constant(argName: String))) = annot.argument(0)

          val argIndex = allParams.indexWhere(_.name.toString == argName)


          for(n <- Range(argIndex, allParams.size)) yield{
            cpy.DefDef(defdef)(
              name = defdef.name,
              paramss = List(allParams.take(n)),
              tpt = defdef.tpt,
              rhs = Apply(
                This(defdef.symbol.owner.asClass).select(defdef.symbol),
                allParams.take(n) ++
                Range(n, allParams.size).map(n2 =>
                  This(defdef.symbol.owner.asClass).select(termName(defdef.name.toString + "$default$" + (n2 + 1)))
                )
              )
            )
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
