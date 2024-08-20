package mill.moduledefs

import scala.collection.mutable.ListBuffer

import dotty.tools.dotc.plugins.{StandardPlugin, PluginPhase}
import dotty.tools.dotc.core.Contexts.{Context, ctx}
import dotty.tools.dotc.core.Comments.docCtx
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.Symbols.ClassSymbol
import dotty.tools.dotc.core.Symbols.requiredClass
import dotty.tools.dotc.core.Names.termName
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.report
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Annotations.Annotation
import dotty.tools.dotc.ast.tpd.{Template, ValDef, DefDef, Literal, NamedArg}
import dotty.tools.dotc.util.Spans.Span

class AutoOverridePluginDotty extends StandardPlugin {

  override def initialize(options: List[String])(using Context): List[PluginPhase] =
    List(EnableScaladocAnnotation(), AutoOverride())

  val name = "auto-override-plugin"
  val description = "automatically inserts `override` keywords for you"

  /** basically we override each kind of definition to copy over its doc comment to an annotation. */
  private class EnableScaladocAnnotation extends PluginPhase {

    override val phaseName: String = "EmbedScaladocAnnotation"

    override val runsAfter: Set[String] = Set("posttyper")
    override val runsBefore: Set[String] = Set("pickler") // TODO: should the annotation be in TASTY?

    private var _ScalaDocAnnot: ClassSymbol | Null = null
    private def ScalaDocAnnot(using Context): ClassSymbol = {
      val local = _ScalaDocAnnot
      if local == null then {
        val sym = requiredClass(AutoOverridePluginDotty.scaladocAnnotationClassName)
        _ScalaDocAnnot = sym
        sym
      } else local
    }
    private lazy val valueName = termName("value")

    private def cookComment(sym: Symbol, span: Span)(using Context): Unit = {
      for
        docCtx <- ctx.docCtx
        comment <- docCtx.docstring(sym)
      do {
        val text = NamedArg(valueName, Literal(Constant(comment.raw))).withSpan(span)
        sym.addAnnotation(Annotation(ScalaDocAnnot, text, span))
      }
    }

    override def prepareForTemplate(tree: Template)(using Context): Context = {
      cookComment(tree.symbol, tree.span)
      ctx
    }

    override def prepareForValDef(tree: ValDef)(using Context): Context = {
      cookComment(tree.symbol, tree.span)
      ctx
    }

    override def prepareForDefDef(tree: DefDef)(using Context): Context = {
      cookComment(tree.symbol, tree.span)
      ctx
    }

  }

  /** This phase automatically adds the override annotation to methods that require one. */
  private class AutoOverride extends PluginPhase {

    override val runsAfter = Set("posttyper")
    override val runsBefore = Set("crossVersionChecks") // this is where override checking happens

    val phaseName = "auto-override"

    private var _Cacher: ClassSymbol | Null = null
    private def Cacher(using Context): ClassSymbol = {
      val local = _Cacher
      if local == null then {
        val sym = requiredClass(AutoOverridePluginDotty.cacherClassName)
        _Cacher = sym
        sym
      } else local
    }

    private def isCacher(owner: Symbol)(using Context): Boolean =
      if owner.isClass then owner.asClass.baseClasses.exists(_ == Cacher)
      else false

    override def prepareForDefDef(d: DefDef)(using Context): Context = {
      val sym = d.symbol
      if sym.allOverriddenSymbols.count(!_.is(Flags.Abstract)) >= 1
      && !sym.is(Flags.Override)
      && isCacher(sym.owner)
      then
        sym.flags = sym.flags | Flags.Override
      ctx
    }
  }

}

object AutoOverridePluginDotty {

  private val cacherClassName = "mill.moduledefs.Cacher"
  private val scaladocAnnotationClassName = "mill.moduledefs.Scaladoc"

}
