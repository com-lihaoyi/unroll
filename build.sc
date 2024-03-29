import mill._, scalalib._, publish._, scalajslib._, scalanativelib._
import $ivy.`com.github.lolgab::mill-mima::0.1.0`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import com.github.lolgab.mill.mima.{CheckDirection, ProblemFilter, Mima}
import com.github.lolgab.mill.mima.worker.MimaBuildInfo
import com.github.lolgab.mill.mima.IncompatibleSignatureProblem
import com.github.lolgab.mill.mima.worker
import com.github.lolgab.mill.mima.worker.MimaWorkerExternalModule

import scala.util.chaining._

val scala212  = "2.12.18"
val scala213  = "2.13.12"
val scala3  = "3.3.1"

val scalaVersions = Seq(scala212, scala213, scala3)


object unroll extends Cross[UnrollModule](scalaVersions)
trait UnrollModule extends Cross.Module[String]{
  trait InnerScalaModule extends CrossScalaModule {
    def crossValue = UnrollModule.this.crossValue
    override def artifactNameParts: T[Seq[String]] = millModuleSegments.parts.patch(1, Nil, 1)
  }

  trait InnerScalaJsModule extends InnerScalaModule with ScalaJSModule{
    def scalaJSVersion = "1.13.1"
  }

  trait InnerScalaNativeModule extends InnerScalaModule with ScalaNativeModule{
    def scalaNativeVersion = "0.4.14"
  }

  trait InnerPublishModule extends InnerScalaModule with PublishModule{

    def publishVersion = VcsVersion.vcsState().format()

    def pomSettings = PomSettings(
      description = "Main method argument parser for Scala",
      organization = "com.lihaoyi",
      url = "https://github.com/com-lihaoyi/unroll",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("com-lihaoyi", "unroll"),
      developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
    )
  }

  object annotation extends InnerPublishModule
  object plugin extends InnerPublishModule{
    def moduleDeps = Seq(annotation)
    def ivyDeps = T{
      if (scalaVersion().startsWith("2.")) Agg(ivy"org.scala-lang:scala-compiler:${scalaVersion()}")
      else  Agg(ivy"org.scala-lang:scala3-compiler_3:${scalaVersion()}")
    }
  }

  object testutils extends InnerScalaModule

  val testcases = Seq(
    "classMethod",
    "objectMethod",
    "traitMethod",
    "genericMethod",
    "curriedMethod",
    "methodWithImplicit",
    "primaryConstructor",
    "secondaryConstructor",
    "caseclass",
    "secondParameterList",
    "abstractTraitMethod",
    "abstractClassMethod"
  )


  object tests extends Cross[Tests](testcases)

  trait Tests extends Cross.Module[String]{
    override def millSourcePath = super.millSourcePath / crossValue
    trait DownstreamModule extends Module{
      def jvm: InnerScalaModule
      def js: InnerScalaJsModule
      def native: InnerScalaNativeModule
    }
    trait CrossPlatformModule extends Module{
      def mimaPrevious = Seq.empty[CrossPlatformModule]
      def moduleDeps: Seq[CrossPlatformModule] = Nil
      object downstream extends DownstreamModule{

        object jvm extends InnerScalaModule with Unrolled {
          def run(args: Task[Args] = T.task(Args())) = T.command{/*donothing*/}
          def moduleDeps = Seq(CrossPlatformModule.this.jvm)
        }
        object js extends InnerScalaJsModule with Unrolled {
          def run(args: Task[Args] = T.task(Args())) = T.command{/*donothing*/}
          def moduleDeps = Seq(CrossPlatformModule.this.js)
        }
        object native extends InnerScalaNativeModule with Unrolled {
          def run(args: Task[Args] = T.task(Args())) = T.command{/*donothing*/}
          def moduleDeps = Seq(CrossPlatformModule.this.native)
        }
      }

      trait Unrolled extends InnerScalaModule with PlatformScalaModule{
        def moduleDeps: Seq[JavaModule] = Seq(annotation)
        def run(args: Task[Args] = T.task(Args())) = T.command{/*donothing*/}
        def mimaPreviousArtifacts = T{
          if (Tests.this.crossValue == "caseclass" && scalaVersion().startsWith("2.")) Nil
          else T.traverse(mimaPrevious)(_.jvm.jar)()
        }
        def scalacPluginClasspath = T{ Agg(plugin.jar()) }

        // override def scalaCompilerClasspath = T{
        //   super.scalaCompilerClasspath().filter(!_.toString().contains("scala3-compiler")) ++
        //   Agg(PathRef(os.Path("/Users/lihaoyi/.ivy2/local/org.scala-lang/scala3-compiler_3/3.3.2-RC3-bin-SNAPSHOT/jars/scala3-compiler_3.jar")))
        // }
        def scalacOptions = T{
          Seq(
            s"-Xplugin:${plugin.jar().path}",
            "-Xplugin-require:unroll",
            //"-Xprint:all",
            //"-Ydebug-error",
            //"-Ydebug-type-error",
            //"-Ydebug-trace"
            //"-Xprint:typer",
            //"-Xprint:unroll",
            //"-Xprint:patmat",
            //"-Xprint:superaccessors"
          )
        }
        trait UnrolledTestModule extends ScalaModule {
          def sources = super.sources() ++ testutils.sources()
          def moduleDeps = Seq(Unrolled.this)
          def mainClass = Some("unroll.UnrollTestMain")
          def testFramework = T{ "" } // stub
        }
      }

      object jvm extends InnerScalaModule with Unrolled with LocalMimaModule{
        def moduleDeps = super.moduleDeps ++ CrossPlatformModule.this.moduleDeps.map(_.jvm.asInstanceOf[Unrolled])
        object test extends ScalaTests with UnrolledTestModule{
          def moduleDeps = Seq(jvm, downstream.jvm)
        }
      }
      object js extends InnerScalaJsModule with Unrolled with LocalMimaModule{
        def moduleDeps = super.moduleDeps ++ CrossPlatformModule.this.moduleDeps.map(_.js.asInstanceOf[Unrolled])
        object test extends ScalaJSTests with UnrolledTestModule{
          def moduleDeps = Seq(js, downstream.js)
        }
      }
      object native extends InnerScalaNativeModule with Unrolled with LocalMimaModule{
        def moduleDeps = super.moduleDeps ++ CrossPlatformModule.this.moduleDeps.map(_.native.asInstanceOf[Unrolled])
        object test extends ScalaNativeTests with UnrolledTestModule{
          def moduleDeps = Seq(native, downstream.native)
        }
      }
    }


    // Different versions of Unrolled.scala
    object v1 extends CrossPlatformModule{
      def mimaPrevious = Seq()
    }

    object v2 extends CrossPlatformModule {
      def mimaPrevious = Seq(v1)
    }

    object v3 extends CrossPlatformModule {
      def mimaPrevious = Seq(v1, v2)
    }

    // proxy modules used to make sure old versions of UnrolledTestMain.scala can
    // successfully call newer versions of the Unrolled.scala
    val items =
      if (!crossValue.startsWith("abstract")) Seq("v3v2", "v3v1", "v2v1")
      else Seq("v3v2", "v3v1", "v2v1", "v2v1v2", "v3v1v3", "v3v1v2", "v3v2v3")

    object cross extends Cross[ComparativeModule](items)
    trait ComparativeModule extends Cross.Module[String]{
      def lookup(x: String): CrossPlatformModule = x match {
        case "1" => v1
        case "2" => v2
        case "3" => v3
      }
      val (upstream, downstream, test) = crossValue match{
        case s"v${a}v${b}v${c}" => (lookup(a), lookup(b), lookup(c))
        case s"v${a}v${b}" => (lookup(a), lookup(a), lookup(b))
      }

      trait ComparativePlatformScalaModule extends PlatformScalaModule{
        def sources = super.sources() ++ testutils.sources()
        def mainClass = Some("unroll.UnrollTestMain")
      }

      object jvm extends InnerScalaModule with ComparativePlatformScalaModule{
        def runClasspath =
          super.runClasspath() ++
            Seq(test.jvm.test.compile().classes, upstream.jvm.compile().classes, downstream.downstream.jvm.compile().classes)

      }

      object js extends InnerScalaJsModule with ComparativePlatformScalaModule{
        def runClasspath =
          super.runClasspath() ++
            Seq(test.js.test.compile().classes, upstream.js.compile().classes, downstream.downstream.js.compile().classes)
      }

      object native extends InnerScalaNativeModule with ComparativePlatformScalaModule{
        def runClasspath =
          super.runClasspath() ++
            Seq(test.native.test.compile().classes, upstream.native.compile().classes, downstream.downstream.native.compile().classes)
      }
    }
  }
}

// Fork of the Mima trait from `mill-mima`, to allow us to run MIMA against
// two compilation outputs for testing purposes.
trait LocalMimaModule extends ScalaModule{

  def mimaWorkerClasspath: T[Agg[PathRef]] = T {
    Lib
      .resolveDependencies(
        repositoriesTask(),
        Agg(
          ivy"com.github.lolgab:mill-mima-worker-impl_2.13:${MimaBuildInfo.publishVersion}"
            .exclude("com.github.lolgab" -> "mill-mima-worker-api_2.13")
        ).map(Lib.depToBoundDep(_, mill.main.BuildInfo.scalaVersion)),
        ctx = Some(T.log)
      )
  }

  def mimaWorker2: Task[worker.api.MimaWorkerApi] = T.task {
    val cp = mimaWorkerClasspath()
    MimaWorkerExternalModule.mimaWorker().impl(cp)
  }
  def mimaCurrentArtifact: T[PathRef] = jar()
  def mimaPreviousArtifacts: T[Seq[PathRef]]
  def mimaReportBinaryIssues(): Command[Unit] = T.command {
    val logDebug: java.util.function.Consumer[String] = T.log.debug(_)
    val logError: java.util.function.Consumer[String] = T.log.error(_)
    val logPrintln: java.util.function.Consumer[String] =
      T.log.outputStream.println(_)
    val runClasspathIO =
      runClasspath().view.map(_.path).filter(os.exists).map(_.toIO).toArray
    val current = mimaCurrentArtifact().path.pipe {
      case p if os.exists(p) => p
      case _                 => (T.dest / "emptyClasses").tap(os.makeDir)
    }.toIO

    val previous = mimaPreviousArtifacts().iterator.map {
      case artifact =>
        new worker.api.Artifact(artifact.path.toString, artifact.path.toIO)
    }.toArray

    val checkDirection = worker.api.CheckDirection.Backward

    val errorOpt: java.util.Optional[String] = mimaWorker2().reportBinaryIssues(
      scalaVersion() match{
        case s"2.$x.$y" => s"2.$x"
        case s"3.$x.$y" => s"3"
      },
      logDebug,
      logError,
      logPrintln,
      checkDirection,
      runClasspathIO,
      previous,
      current,
      Array(),
      new java.util.HashMap(),
      new java.util.HashMap(),
      Array(),
      "dev"
    )

    if (errorOpt.isPresent()) mill.api.Result.Failure(errorOpt.get())
    else mill.api.Result.Success(())
  }
}

