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

  override def newTransformer(unit: global.CompilationUnit): global.Transformer = {
    new UnrollTransformer(unit)
  }

  def findUnrollAnnotation(annotations: Seq[Annotation]): List[String] = {
    annotations.toList.filter(_.tpe =:= typeOf[unroll.Unroll]).flatMap { annot =>
      annot.tree.children.tail match {
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
  def generateSingleForwarder(implDef: ImplDef,
                              defdef: DefDef,
                              paramIndex: Int,
                              firstParamList: List[ValDef],
                              otherParamLists: List[List[ValDef]]) = {
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
      List(firstParamList.take(paramIndex).map(copyValDef)) ++ otherParamLists.map(_.map(copyValDef))

    val forwardedValueParams = firstParamList.take(paramIndex).map(p => Ident(p.name).set(p.symbol))

    val defaultCalls = for (p <- Range(paramIndex, firstParamList.size)) yield {
      val mangledName = defdef.name.toString + "$default$" + (p + 1)

      val defaultOwner =
        if (defdef.symbol.isConstructor) implDef.symbol.companionModule
        else implDef.symbol

      val defaultMember = defaultOwner.tpe.member(TermName(scala.reflect.NameTransformer.encode(mangledName)))
      Ident(mangledName).setSymbol(defaultMember).set(defaultMember)
    }

    val forwarderThis = This(defdef.symbol.owner).set(defdef.symbol.owner)

    val forwarderInner =
      if (defdef.symbol.isConstructor) Super(forwarderThis, typeNames.EMPTY).set(defdef.symbol.owner)
      else forwarderThis

    val nestedForwarderMethodTypes = Seq
      .iterate(defdef.symbol.tpe, defdef.vparamss.length + 1){ case MethodType(args, res) => res }
      .drop(1)

    val forwarderCallArgs =
      Seq(forwardedValueParams ++ defaultCalls) ++
      newVParamss.tail.map(_.map( p => Ident(p.name).set(p.symbol)))

    val forwarderCall0 = forwarderCallArgs
      .zip(nestedForwarderMethodTypes)
      .foldLeft(Select(forwarderInner, defdef.name).set(defdef.symbol): Tree){
        case (lhs, (ps, methodType)) => Apply(fun = lhs, args = ps).setType(methodType)
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

  def generateDefForwarders(implDef: ImplDef, defdef: DefDef, s: String) = defdef.vparamss match {
    case Nil => Nil
    case firstParamList :: otherParamLists =>
      val startParamIndex = firstParamList.indexWhere(_.name.toString == s)
      if (startParamIndex == -1) abort("argument to @Unroll must be the name of a parameter")

      for (paramIndex <- Range(startParamIndex, firstParamList.length)) yield {
        generateSingleForwarder(implDef, defdef, paramIndex, firstParamList, otherParamLists)
      }
  }


  class UnrollTransformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {
    def generateDefForwarders2(implDef: ImplDef): List[List[DefDef]] = {
      implDef.impl.body.collect{ case defdef: DefDef =>

        val annotated =
          if (defdef.symbol.isPrimaryConstructor) defdef.symbol.owner
          else if (defdef.symbol.isCaseApplyOrUnapply && defdef.symbol.name.toString == "apply") defdef.symbol.owner.companionClass
          else if (defdef.symbol.isCaseCopy && defdef.symbol.name.toString == "copy") defdef.symbol.owner
          else defdef.symbol

        findUnrollAnnotation(annotated.annotations).flatMap{s =>
          generateDefForwarders(implDef, defdef, s)
        }
      }
    }

    override def transform(tree: global.Tree): global.Tree = {
      tree match{
        case md: ModuleDef =>
          val allNewMethods = generateDefForwarders2(md).flatten

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
          val allNewMethods = generateDefForwarders2(cd).flatten
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

