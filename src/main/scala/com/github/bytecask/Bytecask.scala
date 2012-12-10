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
* Date: 12/27/11
* Time: 5:57 PM
*/

package com.github.bytecask

import com.github.bytecask.Utils._
import com.github.bytecask.Bytes._
import java.util.concurrent.atomic.AtomicInteger

class Bytecask(val dir: String, val name: String = Utils.randomString(8), maxFileSize: Long = IO.DEFAULT_MAX_FILE_SIZE,
               processor: ValueProcessor = PassThru, autoMerge: Boolean = false, maxConcurrentReaders: Int = 10,
               prefixedKeys: Boolean = false) extends Logging {
  lazy val bytecask = this
  val createdAt = System.currentTimeMillis()
  mkDirIfNeeded(dir)
  val io = new IO(dir, maxConcurrentReaders)
  val index = new Index(io, prefixedKeys)
  val splits = new AtomicInteger
  lazy val merger = new Merger(io, index)
  val TOMBSTONE_VALUE = Bytes.EMPTY
  init()

  def init() {
    index.init()
  }

  def put(key: Array[Byte], value: Array[Byte]) {
    checkArgument(key.length > 0, "Key must not be empty")
    checkArgument(value.length > 0, "Value must not be empty")
    val entry = index.get(key)
    io.synchronized {
      val (pos, length, timestamp) = io.appendDataEntry(key, processor.before(value))
      if (entry.nonEmpty && entry.get.isInactive) merger.entryChanged(entry.get)
      index.update(key, pos, length, timestamp)
      if (io.pos > maxFileSize) split()
    }
  }

  def get(key: Array[Byte]) = {
    checkArgument(key.length > 0, "Key must not be empty")
    index.get(key) match {
      case Some(entry) => processor.after(Some(io.readValue(entry)))
      case _ => None
    }
  }

  def delete(key: Array[Byte]) = {
    checkArgument(key.length > 0, "Key must not be empty")
    index.get(key) match {
      case Some(entry) => {
        io.appendDataEntry(key, TOMBSTONE_VALUE)
        index.delete(key)
        if (entry.isInactive) merger.entryChanged(entry)
        entry
      }
      case _ => None
    }
  }

  def close() {
    io.close()
  }

  def destroy() {
    close()
    rmdir(dir)
  }

  def stats(): String = {
    "name: %s, dir: %s, uptime: %s, count: %s, splits: %s, merges: %s"
      .format(name, dir, now - createdAt, count(), splits.get(), merger.mergesCount)
  }

  def split() {
    synchronized {
      index.postSplit(io.split())
    }
    splits.incrementAndGet()
  }

  def merge() {
    synchronized {
      merger.forceMerge()
    }
  }

  def count() = index.size

  override def toString = "{name=%s, dir=%s}".format(name, dir)

  def selfCheck() {
    notImplementedYet()
  }

  def keys() = index.keys

  def values() = {
    val iterator = index.keys.iterator
    new Iterator[Option[Bytes]]() {

      def hasNext = iterator.hasNext

      def next() = {
        val value = get(iterator.next())
        value.orElse(None)
      }
    }
  }

}
