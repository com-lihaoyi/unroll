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

  def copyParam(p: ValDef, parent: Symbol)(using Context) = {
    implicitly[Context].typeAssigner.assignType(
      cpy.ValDef(p)(p.name, p.tpt, p.rhs),
      Symbols.newSymbol(parent, p.name, p.symbol.flags, p.symbol.info)
    )
  }

  def generateSingleForwarder(defdef: DefDef,
                              prevMethodType: MethodType,
                              firstParamList: List[ValDef],
                              otherParamLists: List[List[ValDef]],
                              paramIndex: Int,
                              isCaseApply: Boolean)(using Context) = {
    val truncatedMethodType = MethodType(
      prevMethodType.paramInfos.take(paramIndex),
      prevMethodType.resType
    )

    val forwarderDefSymbol = Symbols.newSymbol(
      defdef.symbol.owner,
      defdef.name,
      defdef.symbol.flags,
      truncatedMethodType
    )

    val newFirstParamss = firstParamList.take(paramIndex).map(copyParam(_, forwarderDefSymbol))
    val newRestParamss = otherParamLists.map(_.map(copyParam(_, forwarderDefSymbol)))

    val defaultCalls = for (n <- Range(paramIndex, firstParamList.size)) yield {
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
  
  def generateDefForwarders(defdef: DefDef)(using Context) = {
    import dotty.tools.dotc.core.NameOps.isConstructorName
    val firstParamList :: otherParamLists = defdef.paramss.asInstanceOf[List[List[ValDef]]]

    val isCaseCopy =
      defdef.name.toString == "copy" && defdef.symbol.owner.is(CaseClass)

    val isCaseApply =
      defdef.name.toString == "apply" && defdef.symbol.owner.companionClass.is(CaseClass)

    val annotated =
      if (defdef.symbol.isPrimaryConstructor || isCaseCopy) defdef.symbol.owner
      else if (isCaseApply) defdef.symbol.owner.companionClass
      else defdef.symbol

    for (annot <- annotated.annotations.find(_.symbol.fullName.toString == "unroll.Unroll")) yield {
      val Some(Literal(Constant(argName: String))) = annot.argument(0)
      val startParamIndex = firstParamList.indexWhere(_.name.toString == argName)
      if (startParamIndex == -1) sys.error("argument to @Unroll must be the name of a parameter")
      
      val prevMethodType = defdef.symbol.info.asInstanceOf[MethodType]
      for (paramIndex <- Range(startParamIndex, firstParamList.size)) yield {
        generateSingleForwarder(
          defdef,
          prevMethodType,
          firstParamList,
          otherParamLists,
          paramIndex,
          isCaseApply
        )
      }
    }

  }
  override def transformTemplate(tmpl: tpd.Template)(using Context): tpd.Tree = {

    def potentialDefDefs = (tmpl.body ++ Seq(tmpl.constr)).collect{ case defdef: DefDef if defdef.paramss.nonEmpty => defdef }

    val newMethods = potentialDefDefs.map(generateDefForwarders)

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
