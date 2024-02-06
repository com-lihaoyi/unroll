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

  def findUnrollAnnotation(annotations: Seq[Annotation]): List[String] = {
    annotations.toList.filter(_.tpe =:= typeOf[unroll.Unroll]).flatMap { annot =>
      annot.tree.children.tail.map(_.asInstanceOf[NamedArg].rhs) match {
        case Seq(Literal(Constant(s: String))) => Some(s)
        case _ => None
      }
    }
  }

  def copyValDef(vd: ValDef) = {
    val newMods = vd.mods.copy(flags = vd.mods.flags ^ Flags.DEFAULTPARAM)
    newStrictTreeCopier.ValDef(vd, newMods, vd.name, vd.tpt, EmptyTree)
      .setSymbol(vd.symbol)
  }

  implicit class Setter[T <: Tree](t: T){
    def set(s: Symbol) = t.setType(s.tpe).setSymbol(s)
  }
  def generateForwarder(implDef: ImplDef,
                        defdef: DefDef,
                        paramIndex: Int,
                        endParamIndex: Int,
                        vparams: List[ValDef],
                        otherVParamss: List[List[ValDef]]) = {
    val forwarderDefSymbol = defdef.symbol.owner.newMethod(defdef.name)
    val symbolReplacements = defdef
      .vparamss
      .flatten
      .map { p =>
          val newPSymbol = forwarderDefSymbol.newValueParameter(p.name)
          newPSymbol.setInfo(p.symbol.tpe)
          p.symbol -> newPSymbol
        }
      .toMap

    val MethodType(originalParams, result) = defdef.symbol.tpe
    val forwarderParams = originalParams.map(symbolReplacements)
    val forwarderMethodType = MethodType(forwarderParams.take(paramIndex), result)
    forwarderDefSymbol.setInfo(forwarderMethodType)

    val newVParamss =
      List(vparams.take(paramIndex).map(copyValDef)) ++ otherVParamss.map(_.map(copyValDef))

    val forwardedValueParams = vparams.take(paramIndex).map(p => Ident(p.name).set(p.symbol))

    val defaultCalls = for (p <- Range(paramIndex, endParamIndex)) yield {
      val mangledName = defdef.name.toString + "$default$" + (p + 1)

      val defaultOwner =
        if (defdef.symbol.isConstructor) implDef.symbol.companionModule
        else implDef.symbol

      val defaultMember = defaultOwner.tpe.member(TermName(scala.reflect.NameTransformer.encode(mangledName)))
      Ident(mangledName).setSymbol(defaultMember).set(defaultMember)
    }

    val thisTree = This(defdef.symbol.owner).set(defdef.symbol.owner)

    val superThisTree =
      if (defdef.symbol.isConstructor) Super(thisTree, typeNames.EMPTY).set(defdef.symbol.owner)
      else thisTree

    val inner = Select(superThisTree, defdef.name).set(defdef.symbol)

    var forwarderCall0 = Apply(fun = inner, args = forwardedValueParams ++ defaultCalls)
      .setType(defdef.symbol.asMethod.returnType)

    for (ps <- newVParamss.tail) {
      forwarderCall0 = Apply(
        fun = forwarderCall0,
        args = ps.map( p => Ident(p.name).set(p.symbol))
      ).setType(typeOf[(String, String) => String])
    }

    val forwarderCall =
      if (!defdef.symbol.isConstructor) forwarderCall0
      else Block(List(forwarderCall0), Literal(Constant(())).setType(typeOf[Unit]))

    val forwarderDef = treeCopy.DefDef(
      defdef,
      mods = defdef.mods,
      name = defdef.name,
      tparams = defdef.tparams,
      vparamss = newVParamss,
      tpt = defdef.tpt,
      rhs = forwarderCall
    ).setSymbol(forwarderDefSymbol)

    val (fromSyms, toSyms) = symbolReplacements.toList.unzip
    forwarderDef.substituteSymbols(fromSyms, toSyms).asInstanceOf[DefDef]

  }

  def getNewMethods0(implDef: ImplDef, defdef: DefDef, s: String) = defdef.vparamss match {
    case Nil => Nil
    case vparams :: otherVParamss =>
      val startParamIndex = vparams.indexWhere(_.name.toString == s)
      val endParamIndex = vparams.size
      if (startParamIndex == -1) abort("argument to @Unroll must be the name of a parameter")

      for (paramIndex <- Range(startParamIndex, endParamIndex)) yield {
        generateForwarder(implDef, defdef, paramIndex, endParamIndex, vparams, otherVParamss)
      }
  }


  class UnrollTransformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {
    def getNewMethods(implDef: ImplDef): List[List[DefDef]] = {
      implDef.impl.body.collect{ case defdef: DefDef =>
        val annotated =
          if (defdef.symbol.isPrimaryConstructor) defdef.symbol.owner
          else defdef.symbol

        findUnrollAnnotation(annotated.annotations).flatMap{s =>
          getNewMethods0(implDef, defdef, s)
        }
      }
    }

    override def transform(tree: global.Tree): global.Tree = {
      tree match{
        case md: ModuleDef =>
          val allNewMethods = getNewMethods(md).flatten

          val classInfoType = md.symbol.moduleClass.info.asInstanceOf[ClassInfoType]
          val newClassInfoType = classInfoType.copy(decls = newScopeWith(allNewMethods.map(_.symbol) ++ classInfoType.decls:_*))

          md.symbol.moduleClass.setInfo(newClassInfoType)
          super.transform(
            treeCopy.ModuleDef(
              md,
              mods = md.mods,
              name = md.name,
              impl = treeCopy.Template(
                md.impl,
                parents = md.impl.parents,
                self = md.impl.self,
                body = md.impl.body ++ allNewMethods
              )
            )
          )
        case cd: ClassDef =>
          val allNewMethods = getNewMethods(cd).flatten
          super.transform(
            treeCopy.ClassDef(
              cd,
              mods = cd.mods,
              name = cd.name,
              tparams = cd.tparams,
              impl = treeCopy.Template(
                cd.impl,
                parents = cd.impl.parents,
                self = cd.impl.self,
                body = cd.impl.body ++ allNewMethods
              )
            )
          )
        case _ => super.transform(tree)
      }
    }
  }
}

