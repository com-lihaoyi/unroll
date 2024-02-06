import mill._, scalalib._
import $ivy.`com.github.lolgab::mill-mima::0.1.0`
import com.github.lolgab.mill.mima.{CheckDirection, ProblemFilter, Mima}
import com.github.lolgab.mill.mima.worker.MimaBuildInfo
import com.github.lolgab.mill.mima.IncompatibleSignatureProblem
import com.github.lolgab.mill.mima.worker
import com.github.lolgab.mill.mima.worker.MimaWorkerExternalModule

import scala.util.chaining._

val scala212  = "2.12.18"
val scala213  = "2.13.12"
val scala3  = "3.3.1"

val scalaVersions = Seq(scala213/*, scala3*/)

object unroll extends Cross[UnrollModule](scalaVersions)
trait UnrollModule extends CrossScalaModule {
  def ivyDeps = T{
    if (scalaVersion().startsWith("2.")) Agg(ivy"org.scala-lang:scala-compiler:${scalaVersion()}")
    else  Agg(ivy"org.scala-lang:scala3-compiler_3:${scalaVersion()}")
  }

  trait InnerScalaModule extends ScalaModule{
    def scalaVersion = UnrollModule.this.scalaVersion()
  }
  object tests extends Cross[Tests](Seq(
    "classMethod",
    "objectMethod",
    "traitMethod",
    "curriedMethod",
    "primaryConstructor",
    "secondaryConstructor",
  ))
  trait Tests extends InnerScalaModule with Cross.Module[String]{
    override def millSourcePath = super.millSourcePath / crossValue

    // Different versions of Unrolled.scala
    object v3 extends Unrolled {
      def mimaPreviousArtifacts = Seq(v2.jar(), v3.jar())
    }
    object v2 extends Unrolled {
      def mimaPreviousArtifacts = Seq(v2.jar())
    }
    object v1 extends Unrolled{
      def mimaPreviousArtifacts = Seq[PathRef]()
    }

    // proxy modules used to make sure old versions of UnrolledTestMain.scala can
    // successfully call newer versions of the Unrolled.scala
    trait ComparativeScalaModule extends InnerScalaModule{
      def mainClass = Some("unroll.UnrollTestMain")
    }

    object v2v3 extends ComparativeScalaModule{
      def unmanagedClasspath = Agg(v2.test.jar(), v3.jar())
    }
    object v1v3 extends ComparativeScalaModule{
      def unmanagedClasspath = Agg(v1.test.jar(), v3.jar())
    }
    object v1v2 extends ComparativeScalaModule{
      def unmanagedClasspath = Agg(v1.test.jar(), v2.jar())
    }

    trait Unrolled extends InnerScalaModule with LocalMimaModule {
      override def run(args: Task[Args] = T.task(Args())) = T.command{/*donothing*/}
      object test extends InnerScalaModule{
        def moduleDeps = Seq(Unrolled.this)
      }

      def moduleDeps = Seq(UnrollModule.this)
      override def scalacPluginClasspath = T{ Agg(UnrollModule.this.jar()) }

//      override def scalaCompilerClasspath = T{
//        super.scalaCompilerClasspath().filter(!_.toString().contains("scala-compiler")) ++
//        Agg(PathRef(os.Path("/Users/lihaoyi/.ivy2/local/org.scala-lang/scala-compiler/2.13.12-bin-SNAPSHOT/jars/scala-compiler.jar")))
//      }
      override def scalacOptions = T{
        Seq(
          s"-Xplugin:${UnrollModule.this.jar().path}",
          "-Xplugin-require:unroll",
          "-Xprint:all"
        )
      }
    }

    def moduleDeps = Seq(v3)
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

