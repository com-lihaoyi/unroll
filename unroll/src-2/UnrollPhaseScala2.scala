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
    def getNewMethods0(implDef: ImplDef, defdef: DefDef, s: String) = {
      val vparams = defdef.vparamss match {
        case Seq(single) => single
        case multiple => abort(
          "@Unroll only supports methods with a single parameter list, not " + multiple.size
        )
      }

      val startParamIndex = vparams.indexWhere(_.name.toString == s)

      val endParamIndex = vparams.size
      if (startParamIndex == -1) abort("argument to @Unroll must be the name of a parameter")

      for (paramIndex <- Range(startParamIndex, endParamIndex)) yield {
        val forwarderDefSymbol = defdef.symbol.owner.newMethod(defdef.name)

        val forwarderParamsSymbols = vparams.take(paramIndex).map { vd =>
          val sym = forwarderDefSymbol.newValueParameter(vd.name)
          sym.setInfo(vd.symbol.tpe)
          sym
        }

        val forwarderMethodType0 = defdef.symbol.tpe.asInstanceOf[MethodType]
        val forwarderMethodType = MethodType(forwarderParamsSymbols, forwarderMethodType0.resultType)
        forwarderDefSymbol.setInfo(forwarderMethodType)

        val newVParamss = List(
          vparams
            .take(paramIndex)
            .zipWithIndex
            .map { case (vd, i) =>
              newStrictTreeCopier.ValDef(
                vd,
                vd.mods.copy(flags = vd.mods.flags ^ Flags.DEFAULTPARAM),
                vd.name,
                vd.tpt,
                EmptyTree
              ).setSymbol(forwarderParamsSymbols(i))
            }
        )

        val forwardedValueParams = vparams
          .take(paramIndex)
          .zipWithIndex.map {
            case (p, i) => Ident(p.name).setType(p.tpe).setSymbol(newVParamss(0)(i).symbol)
          }

        val defaultCalls = {
          val mangledName = defdef.name.toString + "$default$" + (paramIndex + 1)

          val defaultOwner =
            if (defdef.symbol.isConstructor) implDef.symbol.companionModule
            else implDef.symbol
          val defaultMember = defaultOwner.tpe.member(TermName(scala.reflect.NameTransformer.encode(mangledName)))
          Seq(Ident(mangledName).setSymbol(defaultMember).setType(defaultMember.tpe).setSymbol(defaultMember))
        }

        val forwarderCall = if (defdef.symbol.isConstructor) {
          Block(
            List(
              Apply(
                fun = Select(
                  Super(This(defdef.symbol.owner).setType(ThisType(defdef.symbol.owner)).setSymbol(defdef.symbol.owner), typeNames.EMPTY).setType(ThisType(defdef.symbol.owner)).setSymbol(defdef.symbol.owner),
                  defdef.name
                ).setType(defdef.symbol.tpe)
                  .setSymbol(defdef.symbol),
                args = forwardedValueParams ++ defaultCalls
              ).setType(defdef.symbol.asMethod.returnType)
            ),
            Literal(Constant(())).setType(typeOf[Unit])
          ).setType(typeOf[Unit])
        } else {
          Apply(
            fun = Select(
              This(defdef.symbol.owner).setType(ThisType(defdef.symbol.owner)).setSymbol(defdef.symbol.owner),
              defdef.name
            ).setType(defdef.symbol.tpe)
              .setSymbol(defdef.symbol),
            args = forwardedValueParams ++ defaultCalls
          ).setType(defdef.symbol.asMethod.returnType)
        }

        val forwarderDef = treeCopy.DefDef(
          defdef,
          mods = defdef.mods,
          name = defdef.name,
          tparams = defdef.tparams,
          vparamss = newVParamss,
          tpt = defdef.tpt,
          rhs = forwarderCall
        ).setSymbol(forwarderDefSymbol)

        forwarderDefSymbol.setInfo(forwarderDef.symbol.tpe match {
          case MethodType(params, result) => MethodType(params.take(paramIndex), result)
        })
        forwarderDef.symbol = forwarderDefSymbol

        forwarderDef
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

