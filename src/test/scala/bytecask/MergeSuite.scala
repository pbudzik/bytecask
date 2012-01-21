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

class MergeSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("merge") {
    var db = new Bytecask(mkTempDir, maxFileSize = 1024)
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
    ls(db.dir).size should be(4)
    db.merge()
    val s1 = dirSize(db.dir)
    ls(db.dir).size should be(2 + 1)
    println("size before: %s, after: %s ".format(s0, s1))
    assert(s1 < s0)
    assert(db.get("foo2").isEmpty)
    assert(!db.get("foo1").isEmpty)
    assert(db.get("foo5").isEmpty)
    db.merger.merges.get should be(1)
    assert(ls(db.dir).toList.map(_.getName).contains("1h"))
    db.close()
    db = new Bytecask(db.dir)
    println(db.index.getIndex)
    //assert(db.get("foo2").isEmpty)
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
    db.merger.merges.get should be(0)
    db.destroy()
  }

}