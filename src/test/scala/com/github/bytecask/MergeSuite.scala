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

class MergeSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("merge") {
    var db = new Bytecask(mkTempDir, maxFileSize = 1024)
    db.put("4", randomBytes(128))
    db.put("5", randomBytes(128))
    db.put("1", randomBytes(4096))
    db.put("2", randomBytes(4096))
    db.put("3", randomBytes(4096))
    db.delete("2")
    db.delete("3")
    db.delete("4")
    db.delete("5")
    val s0 = dirSize(db.dir)
    ls(db.dir).size should be(4)
    db.merge()
    val s1 = dirSize(db.dir)
    ls(db.dir).size should be(2 + 1)
    println("size before: %s, after: %s ".format(s0, s1))
    assert(s1 < s0)
    assert(db.get("2").isEmpty)
    assert(db.get("3").isEmpty)
    assert(db.get("4").isEmpty)
    assert(db.get("5").isEmpty)
    assert(!db.get("1").isEmpty)
    db.merger.mergesCount.get should be(1)
    assert(ls(db.dir).toList.map(_.getName).contains("1h"))
    db.close()
    db = new Bytecask(db.dir)
    println(db.index.getMap)
    assert(db.get("2").isEmpty)
    assert(db.get("3").isEmpty)
    assert(db.get("4").isEmpty)
    assert(db.get("5").isEmpty)
    assert(!db.get("1").isEmpty)
    db.destroy()
  }

  test("merge2") {
    var db = new Bytecask(mkTempDir, maxFileSize = 1024)
    db.put("11", randomBytes(128))
    db.put("12", randomBytes(1128))
    db.put("1", randomBytes(1024))
    db.put("2", randomBytes(6))
    db.put("3", randomBytes(4096))
    db.delete("11")
    db.delete("12")
    db.delete("2")
    db.delete("3")
    db.merge()
    ls(db.dir).size should be(2 + 1)
    println(collToString(ls(db.dir).toList))
    println(db.index.getMap)
    assert(db.get("11").isEmpty)
    assert(db.get("12").isEmpty)
    assert(db.get("2").isEmpty)
    assert(db.get("3").isEmpty)
    assert(!db.get("1").isEmpty)
    assert(ls(db.dir).toList.map(_.getName).contains("1h"))
    db.close()
    db = new Bytecask(db.dir)
    assert(db.get("11").isEmpty)
    assert(db.get("12").isEmpty)
    assert(db.get("2").isEmpty)
    assert(db.get("3").isEmpty)
    assert(!db.get("1").isEmpty)
    db.destroy()
  }

  test("skip merge") {
    val db = new Bytecask(mkTempDir, maxFileSize = 1024)
    db.put("foo", randomBytes(128))
    val s0 = dirSize(db.dir)
    ls(db.dir).size should be(1)
    db.merge()
    val s1 = dirSize(db.dir)
    ls(db.dir).size should be(1)
    println("size before: %s, after: %s ".format(s0, s1))
    assert(s1 == s0)
    assert(!db.get("foo").isEmpty)
    db.merger.mergesCount.get should be(0)
    db.destroy()
  }

}