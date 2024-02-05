import mill._
import scalalib._

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

  object tests extends Cross[Tests](Seq("cls", "obj"))
  trait Tests extends ScalaModule with Cross.Module[String]{
    override def millSourcePath = super.millSourcePath / crossValue
    object unrolled extends ScalaModule{
      def moduleDeps = Seq(UnrollModule.this)
      def scalaVersion = UnrollModule.this.scalaVersion()
      override def scalacPluginClasspath = T{ Agg(UnrollModule.this.jar()) }

      override def scalacOptions = T{
        Seq(s"-Xplugin:${UnrollModule.this.jar().path}", "-Xplugin-require:unroll"/*, "-Xprint:all"*/)
      }
    }

    def moduleDeps = Seq(unrolled)
    def scalaVersion = UnrollModule.this.scalaVersion()
  }
}

