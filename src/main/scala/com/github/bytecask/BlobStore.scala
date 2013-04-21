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
* Date: 4/20/13
* Time: 5:50 PM
*/

package com.github.bytecask

import java.io.{OutputStream, InputStream}
import Bytes._
import java.nio.ByteBuffer

trait BlobStore {
  val bytecask: Bytecask

  val blockSize: Int

  def storeBlob(name: String, is: InputStream) {
    val buffer = new Array[Byte](blockSize)
    var blocks = 0
    var totalRead = 0L
    var read = is.read(buffer, 0, blockSize)
    while (read > 0) {
      bytecask.put(key(name, blocks), buffer.slice(0, read))
      blocks = blocks + 1
      totalRead = totalRead + read
      read = is.read(buffer, 0, blockSize)
    }
    storeDescriptor(name, blocks, totalRead)
  }

  def retrieveBlob(name: String, os: OutputStream) {
    bytecask.get(key(name)) match {
      case Some(descriptor) =>
        val buffer = ByteBuffer.wrap(descriptor)
        val blocks = buffer.getInt
        val length = buffer.getLong
        for (i <- 0 to blocks - 1) {
          val buf = bytecask.get(key(name, i)).get
          os.write(buf)
        }
      case _ => throw new IllegalArgumentException("Blob not found: " + name)
    }
  }

  def getBlobMetadata(name: String) = bytecask.get(key(name)) match {
    case Some(descriptor) =>
      val buffer = ByteBuffer.wrap(descriptor)
      val blocks = buffer.getInt
      val length = buffer.getLong
      BlobMeta(name, length, blocks)
    case _ => throw new IllegalArgumentException("Blob not found: " + name)
  }

  private def key(name: String) = "file://" + name

  private def key(name: String, i: Int): String = key(name) + "_" + i

  private def storeDescriptor(name: String, blocks: Int, length: Long) {
    val value = ByteBuffer.allocate(12).putInt(blocks).putLong(length).array()
    bytecask.put(key(name), value)
  }
}

case class BlobMeta(name: String, length: Long, blocks: Int)