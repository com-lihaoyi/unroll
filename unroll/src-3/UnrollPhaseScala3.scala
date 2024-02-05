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

  private var enterSym: Symbol = _

  override val runsAfter = Set(transform.Pickler.name)


  override def transformTemplate(tmpl: tpd.Template)(using Context): tpd.Tree = {
    val newMethods = tmpl.body.collect{
      case d: DefDef =>
        val allParams = d.paramss.asInstanceOf[List[List[ValDef]]].flatten
        for(annot <- d.symbol.annotations.find(_.symbol.fullName.toString == "unroll.Unroll")) yield {
          val Some(Literal(Constant(argName: String))) = annot.argument(0)
          println("argName " + argName)

          val argIndex = allParams.indexWhere(_.name.toString == argName)
          println("argIndex " + argIndex)

          for(n <- Range(argIndex, allParams.size)) yield{
            cpy.DefDef(d)(
              name = d.name,
              paramss = List(allParams.take(n)),
              tpt = d.tpt,
              rhs = Apply(
                Ident(NamedType(tmpl.tpe, d.symbol)),
                allParams.take(n) ++
                  Seq(Ident(NamedType(tmpl.tpe, termName(d.name.toString + "$default$" + (n + 1)))))
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
