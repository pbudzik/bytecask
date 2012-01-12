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
* Date: 7/2/11
* Time: 12:07 PM
*/

package bytecask

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import bytecask.Utils._
import bytecask.Bytes._
import bytecask.Files._

class BasicSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("basic ops") {
    val db = new Bytecask(mkTempDir)
    db.put("foo", "bar")
    db.put("baz", "boo")
    string(db.get("foo").get) should be("bar")
    string(db.get("baz").get) should be("boo")
    db.delete("foo")
    db.get("foo") should be(None)
    db.destroy()
  }

  test("bulk seq put and get") {
    val db = new Bytecask(mkTempDir)
    val length = 2048
    val bytes = randomBytes(length)
    val n = 1000
    throughput("bulk put of " + n + " items", n, n * length) {
      for (i <- 1 to n) (db.put("key_" + i, bytes))
    }
    db.count() should be(n)
    throughput("bulk get of " + n + " items", n, n * length) {
      for (i <- 1 to n) {
        string(db.get("key_" + i).get).length should be(length)
      }
    }
    db.destroy()
  }

  test("bulk par put and get") {
    val db = new Bytecask(mkTempDir)
    val length = 2048
    val bytes = randomBytes(length)
    val n = 1000

    val entries = (for (i <- 1 to n) yield ("key_" + i -> bytes)).toMap

    entries.par.foreach {
      entry =>
        db.put(entry._1, entry._2)
    }

    db.count() should be(n)

    entries.par.foreach {
      entry => assert(!db.get(entry._1).isEmpty, "value is not empty")
    }

    db.count() should be(n)

    db.destroy()
  }

  test("index rebuild") {
    val dir = mkTempDir
    var db = new Bytecask(dir)
    db.put("foo", "bar")
    db.put("baz", "tar")
    db.put("bav", "arc")
    db.put("bav", "arv")
    db.count() should be(3)
    string(db.get("bav").get) should be("arv")
    db.close()
    db = new Bytecask(dir)
    db.count() should be(3)
    string(db.get("bav").get) should be("arv")
    db.destroy()
  }

  test("split") {
    val dir = mkTempDir
    var db = new Bytecask(dir, maxFileSize = 1024)
    db.put("foo", randomBytes(4096))

    db.count() should be(1)
    println("~~~ " + ls(dir).map(_.getName).toList)

    db.close()

    ls(dir).size should be(2)

    db = new Bytecask(dir, maxFileSize = 1024)
    println("~~~ " + ls(dir).map(_.getName).toList)
    println("~~~ " + db.index.getIndex)

    println(db.get("foo"))

    db.put("bar", randomBytes(4096))

    db.close()

    ls(dir).size should be(3)

    db = new Bytecask(dir, maxFileSize = 1024)

    println("~~~ " + ls(dir).map(_.getName).toList)
    println("~~~ " + db.index.getIndex)

    println(db.get("bar"))

    db.destroy()
  }

  test("compaction") {
    val db = new Bytecask(mkTempDir, maxFileSize = 1024, minFileSizeToCompact = 1, dataCompactThreshold = 100)
    db.put("foo4", randomBytes(128))
    db.put("foo5", randomBytes(128))
    db.put("foo1", randomBytes(4096))
    db.put("foo2", randomBytes(4096))
    db.put("foo3", randomBytes(4096))
    db.delete("foo2")
    db.delete("foo3")
    db.delete("foo4")
    db.delete("foo5")
    val s0 = dirSize(db.dir)
    db.compactCheck()
    val s1 = dirSize(db.dir)
    println("sizes: " + s0 + " " + s1)
    db.close()
  }

}