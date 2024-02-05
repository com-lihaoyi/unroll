package counter
object Counter {
  private var count: Array[Long] = null

  def init(num: Int) =
    count = new Array(num)

  def enter(id: Int): Unit =
    count(id) += 1

  def dump(outputFile: String) = {
    val file = new java.io.File(outputFile)
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(file))
    (0 until count.size).foreach { id =>
      bw.write(id.toString + ", " + count(id) + "\n")
    }
    bw.close()
  }
}