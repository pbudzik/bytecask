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
* Date: 1/8/12
* Time: 9:50 AM
*/

package benchmark

import bytecask.Utils._
import bytecask.Bytes._
import util.Random
import bytecask.{Compressor, Bytecask}

object Benchmark {

  def main(args: Array[String]) {
    println("\n--- Benchmark 1 (w/o compressing)...\n")
    //val dir = mkTmpDir.getAbsolutePath
    val dir = "/media/ext/tmp/benchmark_" + now
    var db = new Bytecask(dir)

    warmup(db)
    routine(db)
    println(db.stats())
    db.destroy()

    println("\n--- Benchmark 2 (w/ compressing)...\n")
    db = new Bytecask(dir, processor = Compressor)
    warmup(db)
    routine(db)
    println(db.stats())
    db.destroy()
  }

  def routine(db: Bytecask) {
    println("+ db: %s\n".format(db))
    var n = 10000
    var length = 2048
    val bytes = randomBytes(length)
    throughput("sequential put of different %s items".format(n), n, n * length) {
      for (i <- 1 to n) db.put("key_" + i, bytes)
    }
    throughput("sequential get of different %s items".format(n), n, n * length) {
      for (i <- 1 to n) {
        db.get("key_" + i)
      }
    }
    throughput("sequential get of random items %s times".format(n), n, n * length) {
      val random = new Random(now)
      for (i <- 1 to n) {
        db.get("key_" + random.nextInt(n))
      }
    }
    throughput("sequential get of the same item %s times".format(n), n, n * length) {
      for (i <- 1 to n) {
        db.get("key_" + 10)
      }
    }
    println()
    val entries = (for (i <- 1 to n) yield ("key_" + i -> bytes)).toMap
    throughput("paralell put of different %s items".format(n), n, n * length) {
      entries.par.foreach(entry => db.put(entry._1, entry._2))
    }

    throughput("paralell get of the same item %s times".format(n), n, n * length) {
      entries.par.foreach(entry => db.get("key_100"))
    }

    throughput("paralell get of random %s items".format(n), n, n * length) {
      entries.par.foreach(entry => db.get(entry._1))
    }
  }

  def warmup(db: Bytecask) {
    val bytes = randomBytes(1024)
    for (i <- 1 to 100) db.put("test_key" + i, bytes)
    for (i <- 1 to 100) db.get("test_key" + i)
  }
}