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
* Date: 4/1/13
* Time: 4:30 PM
*/

package com.github.bytecask

/**
 * Eviction to keep max N entries and remove the oldest as first (LIFO)
 */

trait Eviction {

  val bytecask: Bytecask

  val maxCount: Int

  /*
    If there are items beyond the limit, find the oldest and remove
 */

  def performEviction() {
    if (maxCount > 0) {
      val toEvict = bytecask.count() - maxCount
      if (toEvict > 0) {
        val items = bytecask.keys().map(key => (key, bytecask.getMetadata(key).get.timestamp)).toArray.sortBy(t => t._2)
        for (item <- items.take(toEvict))
          bytecask.delete(item._1)
      }
    }
  }


}