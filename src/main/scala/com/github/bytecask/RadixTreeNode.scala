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
* Date: 5/3/12
* Time: 10:57 AM
*/

package com.github.bytecask

import collection.mutable.ArrayBuffer
import com.google.common.base.Objects

class RadixTreeNode[T](var key: String = "", var value: Option[T] = None, var children: ArrayBuffer[RadixTreeNode[T]] = ArrayBuffer[RadixTreeNode[T]]()) {

  def isVirtual = value.isEmpty

  def isRoot = (key == "")

  def setVirtual() {
    value = None
  }

  def longestCommonPrefix(prefix: String) = (prefix, key).zipped.takeWhile(Function.tupled(_ == _)).size

  override def toString = key

  override def hashCode = Objects.hashCode(classOf[RadixTreeNode[T]], key)

}