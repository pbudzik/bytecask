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
import java.io.RandomAccessFile

class FaultTolerantSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach with TestSupport {

  test("corrupted file during indexing - read until error") {
    val dir = mkTempDir
    var db = new Bytecask(dir)
    db.put("1", randomBytes(1024))
    db.put("2", randomBytes(1024))
    db.close()
    val raf = new RandomAccessFile(ls(dir).head, "rw")
    raf.seek(1024 + 32)
    raf.write(randomBytes(128))
    raf.close()
    db = new Bytecask(dir)
    db.index.size should be(1)
    assert(!db.get("1").isEmpty)
    db.index.errorCount() should be(1)
    db.destroy()
  }


}