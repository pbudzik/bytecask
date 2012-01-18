/*
* Copyright 2011 P.Budzik
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* User: przemek
* Date: 1/6/12
* Time: 6:01 PM
*/

package bytecask

import org.xerial.snappy.Snappy
import java.util.concurrent.atomic.AtomicLong
import bytecask.Files.BoostedReader
import java.io.{RandomAccessFile, IOException, File}

object Utils {

  val counter = new AtomicLong

  @inline
  def now = System.currentTimeMillis()

  def mkTempDir = {
    val file = new File(System.getProperty("java.io.tmpdir") + File.separator + "_" + now + "_" + counter.incrementAndGet())
    file.mkdirs()
    file
  }

  def collToString(col: Iterable[Any]) = {
    "[" + col.mkString(",") + "]"
  }

  def randomBytes(k: Int) = Bytes(randomString(k))

  def randomString(k: Int) = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    def s: Stream[Char] = Stream.cons(chars(util.Random.nextInt(chars.size)), s)
    (s take k).mkString
  }

  def rmdir(dir: String) {
    if (System.getProperty("os.name") == "Linux") {
      Runtime.getRuntime.exec("rm -rf " + dir).waitFor()
    } //TODO
  }

  implicit def dslify(s: String) = new DslIfiedString(s)

  def ls(dir: String) = dir.mkFile.listFiles()

  def dirSize(dir: String) = ls(dir).map(_.length()).sum

  def mkDirIfNeeded(dir: String) {
    if (!dir.mkFile.exists()) dir.mkFile.mkdirs()
  }

  def string(bytes: Bytes) = new String(bytes)

  /**
   * File path DSL - instead of + "/" + ... all the parts are glued by / being a separator
   */

  final class DslIfiedString(s: String) {
    def mkFile = new File(s)

    def /(p: String) = {
      s + File.separatorChar + p
    }

    def /(i: Int) = s + File.separatorChar + i
  }

  def time(name: String)(f: => Any) {
    val t0 = now
    f
    println("*** %s time: %s ms".format(now - t0))
  }

  def throughput(name: String, n: Int, length: Int)(f: => Any) {
    val t0 = now
    val result = f
    val time = now - t0
    println("*** %s: time: %s ms, 1 op: %s ms, throughput: %s TPS at %3.2f MB/s, total: %3.2f MB"
      .format(name, time, (time * 1.0) / n, ((n * 1000) / time), (1000.0 * ((length / (1024.0 * 1024.0) / time))), length / (1024.0 * 1024.0)))
    result
  }

  @inline
  def compress(b: Bytes) = Snappy.compress(b)

  @inline
  def uncompress(b: Bytes) = Snappy.uncompress(b)

  @inline
  def processorsNum = Runtime.getRuntime.availableProcessors()

  def checkArgument(condition: Boolean, message: String) {
    if (!condition) throw new IllegalArgumentException(message)
  }

  def notImplementedYet[A](): A = {
    throw new RuntimeException("Not yet implemented")
  }

  def withResource[X <: {def close()}, A](resource: X)(f: X => A) = {
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }

  def withPooled[A](pool: RandomAccessFilePool, file: String)(f: RandomAccessFile => A) = {
    val reader = pool.get(file)
    try {
      f(reader)
    } finally {
      pool.release(file, reader)
    }
  }

  def firstSlot(a: Array[Int]): Option[Int] = {
    for (i <- 0.to(a.size - 2)) {
      if (a(i) + 1 < a(i + 1))
        return Some((a(i) + 1))
    }
    None
  }
}

object Files {
  implicit def fileToString(file: File) = file.getAbsolutePath

  type Reader = {def read(bytes: Array[Byte]): Int}

  class BoostedReader(reader: Reader) {
    def readOrThrow(bytes: Array[Byte], message: String) = {
      val read = reader.read(bytes)
      if (read < 0) throw new IOException(message) else read
    }
  }

  implicit def boostedReader(reader: Reader) = new BoostedReader(reader)
}

