import mill._, scalalib._

val scala212  = "2.12.18"
val scala213  = "2.13.11"

val scala2JVMVersions = Seq(scala212, scala213)
val scalaVersions = scala2JVMVersions

object unroll extends Cross[UnrollModule](scalaVersions)
trait UnrollModule extends CrossScalaModule {
  def ivyDeps = Agg(ivy"org.scala-lang:scala-compiler:${scalaVersion}")
  object test extends ScalaTests with TestModule.Utest{
    override def scalacPluginClasspath = T{ Agg(UnrollModule.this.jar()) }

    override def scalacOptions = T{
      Seq(s"-Xplugin:${UnrollModule.this.jar().path}", "-Xplugin-require:unroll", "-Xprint:all")
    }
  }
}
