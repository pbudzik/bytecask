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
import com.github.bytecask.State._

class StateAwareSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("testing state after put and delete") {
    val db = new Bytecask(mkTempDir) with StateAware
    db.getState should be(Unmodified)
    db.put("foo", "bar")
    db.getState should be(AfterPut)
    db.delete("foo")
    db.getState should be(AfterPutAndDelete)
    db.put("2foo", "bar")
    db.getState should be(AfterPutAndDelete)
    db.resetState()
    db.getState should be(Unmodified)
    db.put("foo", "bar")
    db.resetState()
    db.delete("foo")
    db.getState should be(AfterDelete)
    db.resetState()
    db.getState should be(Unmodified)
    db.destroy()
  }

}