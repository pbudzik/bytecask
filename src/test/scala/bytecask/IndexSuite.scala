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
* Date: 7/2/11
* Time: 12:07 PM
*/

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import bytecask.Utils._
import bytecask.Bytes._
import bytecask.Files._

class IndexSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("index rebuild") {
    val dir = mkTempDir
    var db = new Bytecask(dir)
    db.put("foo", "bar")
    db.put("baz", "tar")
    db.put("bav", "arc")
    db.put("bav", "arv")
    db.count() should be(3)
    string(db.get("bav").get) should be("arv")
    db.delete("baz")
    db.close()
    db = new Bytecask(dir)
    println("index: " + db.index.getIndex)
    db.count() should be(2)
    assert(db.get("baz").isEmpty)
    string(db.get("bav").get) should be("arv")
    db.destroy()
  }

}