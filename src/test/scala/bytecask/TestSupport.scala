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
* Date: 1/10/12
* Time: 7:53 PM
*/

package bytecask

import java.util.concurrent.{TimeUnit, Executors}

trait TestSupport {

  def concurrently(threads: Int, iters: Int = 1, timeout: Int = 5)(f: Int => Any) {
    val pool = Executors.newFixedThreadPool(threads)
    for (i <- 1.to(iters))
      pool.execute(new Runnable() {
        override def run() {
          f
        }
      })
    pool.awaitTermination(timeout, TimeUnit.SECONDS)
    pool.shutdown()
  }

}