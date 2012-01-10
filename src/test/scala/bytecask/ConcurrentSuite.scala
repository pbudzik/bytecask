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
import java.util.concurrent.{TimeUnit, Executors}

class ConcurrentSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("put/get") {
    val db = new Bytecask(mkTmpDir.getAbsolutePath)
    val threads = 1000
    val iters = 100

    concurrently(threads, iters, 5) {
      i => db.put(i.toString, randomBytes(1024))
    }

    concurrently(threads, iters, 5) {
      i => assert(!db.get(i.toString).isEmpty)
    }

    db.destroy()
  }

  def concurrently(threads: Int, iters: Int, timeout: Int)(f: Int => Any) {
    val pool = Executors.newFixedThreadPool(threads)
    for (i <- 1.to(iters))
      pool.execute(new Runnable() {
        override def run() {
          f
        }
      })
    pool.awaitTermination(timeout, TimeUnit.SECONDS)
  }
}