// imports
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.main.RunScript
import mill.eval.Evaluator
import mill.define.{SelectMode, Command}

object Settings {
  val version = "0.10.9"
  val pomOrg = "com.lihaoyi"
  val githubOrg = "com-lihaoyi"
  val githubRepo = "mill-moduledefs"
  val projectUrl = s"https://github.com/${githubOrg}/${githubRepo}"
}

object Deps {
  val scalaVersions = 0.to(13).map(v => "2.13." + v)
  val scalaVersion = scalaVersions.reverse.head
  def scalaCompiler(scalaVersion: String) = ivy"org.scala-lang:scala-compiler:${scalaVersion}"
  val sourcecode = ivy"com.lihaoyi::sourcecode:0.3.0"
}

trait ModuledefsBase extends ScalaModule with PublishModule {
  def publishVersion = Settings.version
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = Settings.pomOrg,
    url = Settings.projectUrl,
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(Settings.githubOrg, Settings.githubRepo),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"),
      Developer("lefou", "Tobias Roeser", "https://github.com/lefou")
    )
  )
  override def javacOptions = Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8")
}

object moduledefs extends ModuledefsBase {
  override def artifactName = "mill-" + super.artifactName()
  override def scalaVersion = Deps.scalaVersion
  override def ivyDeps = Agg(
    Deps.scalaCompiler(scalaVersion()),
    Deps.sourcecode
  )

  object plugin extends Cross[PluginCross](Deps.scalaVersions: _*)
  class PluginCross(override val crossScalaVersion: String) extends CrossScalaModule
      with ModuledefsBase {
    override def artifactName = "scalac-mill-" + super.artifactName()
    override def moduleDeps = Seq(moduledefs)
    override def crossFullScalaVersion = true
    override def ivyDeps = Agg(
      Deps.scalaCompiler(scalaVersion()),
      Deps.sourcecode
    )
  }
}

def publishToSonatype(
    sonatypeCreds: String,
    gpgArgs: String = PublishModule.defaultGpgArgs.mkString(","),
    dryRun: Boolean = false,
    artifactsFile: Option[String] = None
): Command[Unit] = {

  val pubTasks: Seq[String] = artifactsFile.map(f => os.Path(f, os.pwd)).filter(os.exists) match {
    case None => Seq("__.publishArtifacts")
    case Some(f) =>
      val tasks = os.read.lines(f)
        .map(_.trim())
        .filter(l => l.nonEmpty && !l.startsWith("#"))
      if (tasks.isEmpty) sys.error(s"No artifacts tasks selected. File ${f} cannot be empty.")
      tasks
  }

  val Right(tasks) = RunScript.resolveTasks(
    mill.main.ResolveTasks,
    Evaluator.currentEvaluator.get,
    pubTasks,
    SelectMode.Single
  )

  T.command {
    val pubArtifacts: Seq[(Seq[(os.Path, String)], Artifact)] = T.sequence(tasks)().map {
      case PublishModule.PublishData(a, s) => (s.map { case (p, f) => (p.path, f) }, a)
    }

    T.log.debug(s"Publishing artifacts: ${pubArtifacts.map(_._2).mkString("\n  ", "\n  ", "")}")

    if (dryRun) {
      T.log.info(
        s"Skipping publish for artifacts: ${pubArtifacts.map(_._2).mkString("\n  ", "\n  ", "")}"
      )
    } else {
      new SonatypePublisher(
        uri = "https://oss.sonatype.org/service/local",
        snapshotUri = "https://oss.sonatype.org/content/repositories/snapshots",
        sonatypeCreds,
        signed = true,
        gpgArgs.split(','),
        readTimeout = 6000000,
        connectTimeout = 600000,
        T.log,
        T.workspace,
        T.env,
        awaitTimeout = 600000,
        stagingRelease = true
      ).publishAll(
        release = true,
        pubArtifacts: _*
      )
    }
  }

}
