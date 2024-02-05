import mill._, scalalib._

val scala212  = "2.12.18"
val scala213  = "2.13.11"
val scala3  = "3.3.1"

val scalaVersions = Seq(scala213, scala3)

object unroll extends Cross[UnrollModule](scalaVersions)
trait UnrollModule extends CrossScalaModule {
  def ivyDeps = T{
    if (scalaVersion().startsWith("2.")) Agg(ivy"org.scala-lang:scala-compiler:${scalaVersion()}")
    else  Agg(ivy"org.scala-lang:scala3-compiler_3:${scalaVersion()}")
  }

  object test extends ScalaTests with TestModule.Utest{
    override def scalacPluginClasspath = T{ Agg(UnrollModule.this.jar()) }

    override def scalacOptions = T{
      Seq(s"-Xplugin:${UnrollModule.this.jar().path}", "-Xplugin-require:unroll"/*, "-Xprint:all"*/)
    }
  }
}
