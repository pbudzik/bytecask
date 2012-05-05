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

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatest.matchers.ShouldMatchers

class RadixTreeIndexMapSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  var map: RadixTreeMap[Int] = _

  test("basic") {
    assert(map.size == 0)
    assert(map.iterator.hasNext == false)
    map.put("1", 1)
    assert(map.iterator.hasNext == true)
    assert(map.size == 1)
    map.remove("1")
    assert(map.size == 0)
    map.put("1", 2)
    assert(map.get("1") == Some(2))
    assert(map.get("") == None)
    assert(map.get("2") == None)
    map.put("...", 0)
    val m = (for ((k, v) <- map.iterator) yield (k, v)).toMap
    assert(m.size == 2)
    assert(!map.contains("abc"))
    assert(map.keys.size == 2)
    assert(map.values.size == 2)
    assert(map.keys.map(b => new String(b.bytes)).toSet == Set("...", "1"))
  }

  test("many items") {
    for (i <- 1.to(99999)) {
      map.put(System.currentTimeMillis() + "item:" + i, i)
    }
    val key = System.currentTimeMillis() + "item:"
    map.put(key, 0)
    map.put("foo", 1)
    val m = (for ((k, v) <- map.iterator) yield (k, v)).toMap
    assert(m.size == 99999 + 2)
    assert(map.get(key) == Some(0))
    assert(map.get("foo") == Some(1))
    map.remove(key)
    assert(map.get(key) == None)
    map.put(key, 333)
    assert(map.get(key) == Some(333))
  }

  override def beforeEach() {
    map = new RadixTreeMap[Int]()
  }


}