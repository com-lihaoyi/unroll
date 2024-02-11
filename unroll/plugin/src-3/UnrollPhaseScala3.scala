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
import dotty.tools.dotc.core.Types.{MethodType, NamedType, PolyType, Type}
import dotty.tools.dotc.core.Symbols

import scala.language.implicitConversions

class UnrollPhaseScala3() extends PluginPhase {
  import tpd._

  val phaseName = "unroll"

  override val runsAfter = Set(transform.Pickler.name)

  def copyParam(p: ValDef, parent: Symbol)(using Context) = {
    implicitly[Context].typeAssigner.assignType(
      cpy.ValDef(p)(p.name, p.tpt, p.rhs),
      Symbols.newSymbol(parent, p.name, p.symbol.flags, p.symbol.info)
    )
  }

  def copyParam2(p: TypeDef, parent: Symbol)(using Context) = {
    implicitly[Context].typeAssigner.assignType(
      cpy.TypeDef(p)(p.name, p.rhs),
      Symbols.newSymbol(parent, p.name, p.symbol.flags, p.symbol.info)
    )
  }

  def generateSingleForwarder(defdef: DefDef,
                              prevMethodType: Type,
                              paramLists: List[ParamClause],
                              firstValueParamClauseIndex: Int,
                              paramIndex: Int,
                              isCaseApply: Boolean)(using Context) = {

    def truncateMethodType0(tpe: Type): Type = {
      tpe match{
        case pt: PolyType => PolyType(pt.paramInfos, truncateMethodType0(pt.resType))
        case mt: MethodType => MethodType(mt.paramInfos.take(paramIndex), mt.resType)
      }
    }

    val truncatedMethodType = truncateMethodType0(prevMethodType)

    val forwarderDefSymbol = Symbols.newSymbol(
      defdef.symbol.owner,
      defdef.name,
      defdef.symbol.flags &~ HasDefaultParams,
      truncatedMethodType
    )

    val updated: List[ParamClause] = paramLists.zipWithIndex.map{ case (ps, i) =>
      if (i == firstValueParamClauseIndex) ps.take(paramIndex).map(p => copyParam(p.asInstanceOf[ValDef], forwarderDefSymbol))
      else {
        if (ps.headOption.exists(_.isInstanceOf[TypeDef])) ps.map(p => copyParam2(p.asInstanceOf[TypeDef], forwarderDefSymbol))
        else ps.map(p => copyParam(p.asInstanceOf[ValDef], forwarderDefSymbol))
      }
    }

    val defaultCalls = for (n <- Range(paramIndex, paramLists(firstValueParamClauseIndex).size)) yield {
      if (defdef.symbol.isConstructor) {
        ref(defdef.symbol.owner.companionModule)
          .select(DefaultGetterName(defdef.name, n))
      } else if (isCaseApply) {
        ref(defdef.symbol.owner.companionModule)
          .select(DefaultGetterName(termName("<init>"), n))
      } else {
        This(defdef.symbol.owner.asClass)
          .select(DefaultGetterName(defdef.name, n))
      }
    }

    val allNewParamTrees =
      updated.zipWithIndex.map{case (ps, i) =>
        if (i == firstValueParamClauseIndex) ps.map(p => ref(p.symbol)) ++ defaultCalls
        else ps.map(p => ref(p.symbol))
      }

    val forwarderInner: Tree = This(defdef.symbol.owner.asClass).select(defdef.symbol)

    val forwarderCall0 = allNewParamTrees.foldLeft[Tree](forwarderInner){
      case (lhs: Tree, newParams) =>
        if (newParams.headOption.exists(_.isInstanceOf[TypeTree])) TypeApply(lhs, newParams)
        else Apply(lhs, newParams)
    }

    val forwarderCall =
      if (!defdef.symbol.isConstructor) forwarderCall0
      else Block(List(forwarderCall0), Literal(Constant(())))

    val newDefDef = implicitly[Context].typeAssigner.assignType(
      cpy.DefDef(defdef)(
        name = forwarderDefSymbol.name,
        paramss = updated,
        tpt = defdef.tpt,
        rhs = forwarderCall
      ),
      forwarderDefSymbol
    )

    newDefDef
  }

  def isCaseFromProduct(t: Tree)(using Context) = t match{
    case defdef: DefDef => defdef.name.toString == "fromProduct" && defdef.symbol.owner.companionClass.is(CaseClass)
    case _ => false
  }

  def generateFromProduct(startParamIndex: Int, paramCount: Int, defdef: DefDef)(using Context) = {
    cpy.DefDef(defdef)(
      name = defdef.name,
      paramss = defdef.paramss,
      tpt = defdef.tpt,
      rhs = Match(
        ref(defdef.paramss.head.head.asInstanceOf[ValDef].symbol).select(termName("productArity")),
        Range(startParamIndex, paramCount).toList.map { paramIndex =>
          val Apply(select, args) = defdef.rhs
          CaseDef(
            Literal(Constant(paramIndex)),
            EmptyTree,
            Apply(
              select,
              args.take(paramIndex) ++
                Range(paramIndex, paramCount).map(n =>
                  ref(defdef.symbol.owner.companionModule)
                    .select(DefaultGetterName(defdef.symbol.owner.primaryConstructor.name.toTermName, n))
                )
            )
          )
        } ++ Seq(
          CaseDef(
            EmptyTree,
            EmptyTree,
            defdef.rhs
          )
        )
      )
    ).setDefTree
  }

  def generateSyntheticDefs(tree: Tree)(using Context): Seq[Tree] = tree match{
    case defdef: DefDef if defdef.paramss.nonEmpty =>
      import dotty.tools.dotc.core.NameOps.isConstructorName

      val isCaseCopy =
        defdef.name.toString == "copy" && defdef.symbol.owner.is(CaseClass)

      val isCaseApply =
        defdef.name.toString == "apply" && defdef.symbol.owner.companionClass.is(CaseClass)

      val isCaseFromProduct = this.isCaseFromProduct(defdef)

      val annotated =
        if (isCaseCopy) defdef.symbol.owner.primaryConstructor
        else if (isCaseApply) defdef.symbol.owner.companionClass.primaryConstructor
        else if (isCaseFromProduct) defdef.symbol.owner.companionClass.primaryConstructor
        else defdef.symbol

      val firstValueParamClauseIndex = annotated.paramSymss.indexWhere(!_.headOption.exists(_.isType))

      if (firstValueParamClauseIndex == -1) Nil
      else {
        val paramCount = annotated.paramSymss(firstValueParamClauseIndex).size
        annotated
          .paramSymss(firstValueParamClauseIndex)
          .indexWhere(_.annotations.exists(_.symbol.fullName.toString == "unroll.Unroll")) match{
          case -1 => Nil
          case startParamIndex =>
            if (isCaseFromProduct) {
              Seq(generateFromProduct(startParamIndex, paramCount, defdef))
            } else {
              for (paramIndex <- Range(startParamIndex, paramCount)) yield {
                generateSingleForwarder(
                  defdef,
                  defdef.symbol.info,
                  defdef.paramss,
                  firstValueParamClauseIndex,
                  paramIndex,
                  isCaseApply
                )
            }
          }
        }
    }
    case _ => Nil
  }

  override def transformTemplate(tmpl: tpd.Template)(using Context): tpd.Tree = {

    super.transformTemplate(
      cpy.Template(tmpl)(
        tmpl.constr,
        tmpl.parents,
        tmpl.derived,
        tmpl.self,
        tmpl.body.filter(!this.isCaseFromProduct(_)) ++
        tmpl.body.flatMap(generateSyntheticDefs) ++
        generateSyntheticDefs(tmpl.constr)
      )
    )
  }
}
