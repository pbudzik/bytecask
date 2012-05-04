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
import com.github.bytecask.Files._
import com.github.bytecask.Bytes._

class CompressSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("compressing 64K") {
    val c = randomString(1024 * 64).getBytes
    val d = uncompress(compress(c))
    Bytes(c) should be(Bytes(d))
  }

  test("compressing 1 char long") {
    val c = "1".getBytes
    val d = uncompress(compress(c))
    c should be(d)
  }

  test("basic ops with compression") {
    val db = new Bytecask(mkTempDir, processor = Compressor)
    db.put("foo", "bar")
    db.put("baz", "boo")
    string(db.get("foo").get) should be("bar")
    string(db.get("baz").get) should be("boo")
    db.keys().map(string(_)) should be(Set("foo", "baz"))
    db.values().size should be(2)
    db.delete("foo")
    db.get("foo") should be(None)
    db.destroy()
  }

}