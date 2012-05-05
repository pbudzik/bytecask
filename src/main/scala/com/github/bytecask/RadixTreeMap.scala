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
* Date: 5/4/12
* Time: 7:51 PM
*/

package com.github.bytecask

import collection.mutable.Map
import com.github.bytecask.Bytes._

/**
 * Map to be a drop in replacement for the regular one used by the index.
 * Backed by RadixTree, so if keys are prefixed strings it can save a lot of
 * space taken by keys. Good for cases when keys are i.e. directories/files, IP addresses etc.
 *
 * It is not optimized yet.
 *
 */

class RadixTreeMap[T] extends Map[Bytes, T] {
  val tree = new RadixTree[T]()

  def get(key: Bytes) = tree.find(new String(key.bytes))

  def iterator = new Iterator[(Bytes, T)]() {
    val it = tree.iterator

    def hasNext = it.hasNext

    def next() = {
      val (k, v) = it.next()
      (k, v)
    }

  }

  override def size = tree.size

  def +=(kv: (Bytes, T)) = {
    tree.insert(new String(kv._1.bytes), kv._2)
    this
  }

  def -=(key: Bytes) = {
    tree.delete(new String(key.bytes))
    this
  }

  override def contains(key: Bytes) = !tree.find(new String(key.bytes)).isEmpty

}

class RadixTreeIndexMap extends RadixTreeMap[IndexEntry]
