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

class ConcurrentSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach with TestSupport {

  var db: Bytecask = _

  test("put/get 1K") {
    val threads = 1000
    val iters = 100
    concurrently(threads, iters) {
      i => db.put(i.toString, randomBytes(1024))
    }
    concurrently(threads, iters) {
      i => assert(!db.get(i.toString).isEmpty, "not empty")
    }
    db.count() should be(iters)
  }

  test("put/get 32K") {
    val threads = 1000
    val iters = 100
    concurrently(threads, iters) {
      i => db.put(i.toString, randomBytes(1024 * 32))
    }
    concurrently(threads, iters) {
      i => assert(!db.get(i.toString).isEmpty, "not empty")
    }
    db.count() should be(iters)
  }

  test("put/delete") {
    val threads = 100
    val iters = 100
    concurrently(threads, iters) {
      i => db.put(i.toString, randomBytes(1024))
    }
    concurrently(threads, iters) {
      i => db.delete(i.toString)
    }
    db.count() should be(0)
  }

  test("mixed put/get") {
    val threads = 1000
    val iters = 1000
    db.put("a", randomBytes(1024 * 64))
    concurrently(threads, iters) {
      i => db.put(randomBytes(256), randomBytes(1024))
      assert(!db.get("a").isEmpty, "not empty")
    }
    db.count() should be(iters + 1)
  }

  override def beforeEach() {
    db = new Bytecask(mkTempDir)
  }

  override def afterEach() {
    db.destroy()
  }

}