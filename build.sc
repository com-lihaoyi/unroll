// imports
import mill._
import mill.scalalib._
import mill.scalalib.publish._

object Settings {
  val version = "0.10.9"
  val pomOrg = "com.lihaoyi"
  val githubOrg = "com-lihaoyi"
  val githubRepo = "mill-moduledefs"
  val projectUrl = s"https://github.com/${githubOrg}/${githubRepo}"
}

object Deps {
  val scalaVersions = 0.to(10).map(v => "2.13." + v)
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
  class PluginCross(override val crossScalaVersion: String) extends CrossScalaModule with ModuledefsBase {
    override def artifactName = "scalac-mill-" + super.artifactName()
    override def moduleDeps = Seq(moduledefs)
    override def crossFullScalaVersion = true
    override def ivyDeps = Agg(
      Deps.scalaCompiler(scalaVersion()),
      Deps.sourcecode
    )
  }
}
