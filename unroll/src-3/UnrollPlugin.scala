package unroll
import dotty.tools.dotc.plugins._

class UnrollPlugin extends StandardPlugin {
  val name: String = "unroll"
  override val description: String = "Count method calls"

  def init(options: List[String]): List[PluginPhase] =
    val setting = new Setting(options.headOption)
    (new PhaseA(setting)) :: (new PhaseB(setting)) :: Nil
}