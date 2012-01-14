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

class UtilsSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("slot") {
    firstSlot(Array(1, 2, 3, 5)) should be(Some(4))
    firstSlot(Array(1, 2, 4, 5)) should be(Some(3))
    firstSlot(Array(1, 2, 4, 5)) should be(Some(3))
    firstSlot(Array(1, 15)) should be(Some(2))
    firstSlot(Array(1, 2, 3, 4)) should be(None)
  }


}