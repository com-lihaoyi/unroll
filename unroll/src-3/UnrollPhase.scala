package unroll

import dotty.tools.dotc._

import plugins._

import core._
import Contexts._
import Symbols._
import Flags._
import SymDenotations._

import Decorators._
import ast.Trees._
import ast.tpd
import StdNames.nme
import Names._
import Constants.Constant

import scala.language.implicitConversions


class PhaseA(setting: Setting) extends PluginPhase {
  import tpd._

  val phaseName = "PhaseA"

  private var enterSym: Symbol = _

  override val runsAfter = Set(transform.Pickler.name)
  override val runsBefore = Set("PhaseB")

  override def prepareForUnit(tree: Tree)(using Context): Context =
    val runtime = requiredModule(setting.runtimeObject)
    enterSym = runtime.requiredMethod("enter")
    ctx

  override def transformDefDef(tree: DefDef)(using Context): Tree = {
    val sym = tree.symbol

    // ignore abstract and synthetic methods
    if tree.rhs.isEmpty|| sym.isOneOf(Synthetic | Deferred | Private | Accessor)
    then return tree

    val methId = setting.add(tree)
    val enterTree = ref(enterSym).appliedTo(Literal(Constant(methId)))

    val rhs1 = tpd.Block(enterTree :: Nil, tree.rhs)

    cpy.DefDef(tree)(rhs = rhs1)
  }
}

class PhaseB(setting: Setting) extends PluginPhase {
  import tpd._

  val phaseName: String = "PhaseB"

  override val runsAfter = Set("PhaseA")
  override val runsBefore = Set(transform.Erasure.name)

  private var initSym: Symbol = _
  private var dumpSym: Symbol = _
  private var dumped: Boolean = false

  override def prepareForUnit(tree: Tree)(using Context): Context =
    if !dumped then
      dumped = true
      setting.writeMethods()

    val runtime = requiredModule(setting.runtimeObject)
    initSym = runtime.requiredMethod("init")
    dumpSym = runtime.requiredMethod("dump")
    ctx

  override def transformDefDef(tree: DefDef)(using Context): Tree =
    if ctx.platform.isMainMethod(tree.symbol) then
      val size = setting.methodCount
      val initTree = ref(initSym).appliedTo(Literal(Constant(size)))
      val dumpTree = ref(dumpSym).appliedTo(Literal(Constant(setting.runtimeOutputFile)))
      val rhs1 = Block(initTree :: tree.rhs :: Nil, dumpTree)
      cpy.DefDef(tree)(rhs = rhs1)
    else tree
}