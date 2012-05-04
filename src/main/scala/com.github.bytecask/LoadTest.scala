package bytecask

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

import bytecask.Utils._
import bytecask.Bytes._
import bytecask.Files._
import util.Random
import bytecask.Bytecask

object LoadTest {

  def main(args: Array[String]) {
    val dir = "/media/ext/tmp/benchmark_" + now
    val db = new Bytecask(dir, maxFileSize = 500 * 1024)
    warmup(db)
    for (i <- 1.to(5000))
      putAndGet(db)
    println(db.stats())
    db.destroy()
  }

  private def putAndGet(db: Bytecask) {
    println("+ db: %s\n".format(db))
    val random = new Random(now)
    val n = 10000
    val length = random.nextInt(2048) + 1
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
    throughput("concurrent put of different %s items".format(n), n, n * length) {
      entries.par.foreach(entry => db.put(entry._1, entry._2))
    }

    throughput("concurrent get of the same item %s times".format(n), n, n * length) {
      entries.par.foreach(entry => db.get("key_100"))
    }

    throughput("concurrent get of random %s items".format(n), n, n * length) {
      entries.par.foreach(entry => db.get(entry._1))
    }
  }

  private def warmup(db: Bytecask) {
    val bytes = randomBytes(1024)
    for (i <- 1 to 1000) db.put("test_key" + i, bytes)
    for (i <- 1 to 1000) db.get("test_key" + i)
  }

}