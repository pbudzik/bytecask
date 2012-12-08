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
* Date: 1/6/12
* Time: 5:50 PM
*/

package com.github.bytecask

import java.util.Arrays

/*
Internal byte array representation with equals to compare array values
 */

final class Bytes(val bytes: Array[Byte]) {

  override def equals(other: Any) = {
    if (!other.isInstanceOf[Bytes]) false
    else Arrays.equals(bytes, other.asInstanceOf[Bytes].bytes)
  }

  def size = bytes.size

  override def hashCode = Arrays.hashCode(bytes)
}

object Bytes {
  lazy val EMPTY = Bytes(Array[Byte]())

  def apply(bytes: Array[Byte]) = new Bytes(bytes)

  def apply(s: String) = new Bytes(s.getBytes)

  implicit def arrToBytes(bytes: Array[Byte]) = Bytes(bytes)

  implicit def strToBytes(s: String) = Bytes(s.getBytes)

  implicit def byteToBytes(i: Byte) = Bytes(Array(i))

  implicit def intToBytes(i: Int) = Array(i.toByte)

  implicit def toArray(bytes: Bytes) = bytes.bytes

  implicit def strToArray(s: String) = s.getBytes

}


