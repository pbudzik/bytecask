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

package com.github.bytecask

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import com.github.bytecask.Utils._
import com.github.bytecask.Bytes._
import com.github.bytecask.Files._

class BasicSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("basic ops") {
    val db = new Bytecask(mkTempDir)
    db.put("foo", "bar")
    db.put("baz", "boo")
    string(db.get("foo").get) should be("bar")
    string(db.get("baz").get) should be("boo")
    db.keys().map(string) should be(Set("foo", "baz"))
    assert (db.getMetadata("foo").get.timestamp > 0)
    assert (db.getMetadata("baz").get.length > 0)
    db.values().size should be(2)
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

  test("bulk concurrent put and get") {
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

  test("split") {
    val dir = mkTempDir
    var db = new Bytecask(dir, maxFileSize = 1024)
    db.put("foo", randomBytes(4096))

    db.count() should be(1)
    println("*** " + ls(dir).map(_.getName).toList)

    db.close()

    ls(dir).size should be(2)

    db = new Bytecask(dir, maxFileSize = 1024)
    println("*** " + ls(dir).map(_.getName).toList)
    println("*** " + db.index.getMap)

    println(db.get("foo"))

    db.put("bar", randomBytes(4096))

    db.close()

    ls(dir).size should be(3)

    db = new Bytecask(dir, maxFileSize = 1024)

    println("*** " + ls(dir).map(_.getName).toList)
    println("*** " + db.index.getMap)

    println(db.get("bar"))

    db.destroy()
  }

}