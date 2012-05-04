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
package com.github.bytecask

import com.github.bytecask.Utils._
import com.github.bytecask.Bytes._
import com.github.bytecask.Files._
import util.Random

object Benchmark {

  /**
   *   Warning: it is writing to a temp directory!
   */

  def main(args: Array[String]) {
    b1()
    b2()
    b3()
    b4()
  }

  def b1() {
    println("\n--- Benchmark 1 - small values...\n")
    val dir = mkTempDir
    val db = new Bytecask(dir)
    warmup(db)
    putAndGet(db, 128)
    println(db.stats())
    db.destroy()
  }

  def b2() {
    println("\n--- Benchmark 2 - big values...\n")
    val dir = mkTempDir
    val db = new Bytecask(dir)
    warmup(db)
    putAndGet(db, 1024 * 64)
    println(db.stats())
    db.destroy()
  }

  def b3() {
    println("\n--- Benchmark 3 (splitting)...\n")
    val dir = mkTempDir
    val db = new Bytecask(dir, maxFileSize = 1024 * 20)
    val n = 100
    val length = 2048
    val bytes = randomBytes(length)
    throughput("sequential put of different %s items w/ frequent splits".format(n), n, n * length) {
      for (i <- 1 to n) db.put("k" + i, bytes)
    }
    println("Fetching entries... ")
    for (i <- 1 to n) db.get("k" + i).get
    println("Files in db's directory: %s".format(collToString(ls(dir).map(_.getName))))
    println(db.stats())
    db.destroy()
  }

  def b4() {
    println("\n--- Benchmark 3 (merging)...\n")
    val dir = mkTempDir.getAbsolutePath
    val db = new Bytecask(dir, maxFileSize = 1024 * 10)
    val n = 100
    val length = 2048
    val bytes = randomBytes(length)
    throughput("sequential put of different %s items w/ frequent splits".format(n), n, n * length) {
      for (i <- 1 to n) db.put("k" + i, bytes)
    }
    val random = new Random(now)
    println("sequential delete of random %s items".format(20))
    for (i <- 1 to n) db.delete("k" + random.nextInt(100))
    println("sequential over-put of random %s items".format(20))
    for (i <- 1 to n) db.put("k" + random.nextInt(100), "foo")
    println("Files in db's directory: %s".format(collToString(ls(dir).map(_.getName))))
    val s0 = dirSize(dir)
    println("Directory size: %s bytes".format(s0))
    db.merge()
    val s1 = dirSize(dir)
    println("Directory size: %s bytes".format(s1))
    println("Reduction: %s | %s -> %3.2f".format(s0, s1, 100 - ((s1 * 1.0) / s0) * 100.0) + "%")
    println(db.stats())
    db.destroy()
  }

  private def putAndGet(db: Bytecask, valueSize: Int) {
    println("+ db: %s\n".format(db))
    val n = 10000
    val length = valueSize
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