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

package bytecask

import bytecask.Utils._
import management.ManagementFactory
import javax.management.ObjectName
import bytecask.Bytes._
import java.util.concurrent.atomic.AtomicInteger

class Bytecask(dir: String, name: String = Utils.randomString(8), maxFileSize: Long = Int.MaxValue,
               minFileSizeToCompact: Int = 1024 * 1024, dataCompactThreshold: Int = 1024 * 1024,
               processor: ValueProcessor = PassThru, jmx: Boolean = true)
  extends Logging {
  val createdAt = System.currentTimeMillis()
  mkDirIfNeeded(dir)
  val io = new IO(dir)
  val index = new Index(io)
  val splits = new AtomicInteger
  val compactor = new Compactor(io, index)
  val TOMBSTONE_VALUE = Bytes.EMPTY

  init()

  def init() {
    index.init()
    if (jmx) jmxInit()
  }

  def put(key: Array[Byte], value: Array[Byte]) {
    checkArgument(key.length > 0, "Key cannot be empty")
    checkArgument(value.length > 0, "Value cannot be empty")
    val entry = index.get(key)
    val (pos, length, timestamp) = io.appendEntry(key, processor.before(value))
    if (!entry.isEmpty && entry.get.isInactive) compactor.entryChanged(entry.get)
    index.update(key, pos, length, timestamp)
    if (io.pos > maxFileSize) split()
  }

  def get(key: Array[Byte]) = {
    checkArgument(key.length > 0, "Key cannot be empty")
    val entry = index.get(key)
    if (!entry.isEmpty) processor.after(Some(io.readValue(entry.get))) else None
  }

  def delete(key: Array[Byte]) {
    checkArgument(key.length > 0, "Key cannot be empty")
    val entry = index.get(key)
    if (!entry.isEmpty) {
      io.appendEntry(key, TOMBSTONE_VALUE)
      index.delete(key)
      if (entry.get.isInactive) compactor.entryChanged(entry.get)
      Some(entry)
    } else None
  }

  def close() {
    io.close()
  }

  def destroy() {
    close()
    rmdir(dir)
  }

  def stats(): String = {
    "name: %s, dir: %s, uptime: %s, count: %s, splits: %s, compactions: %s"
      .format(name, dir, now - createdAt, count(), splits.get(), compactor.compactions)
  }

  def split() {
    this.synchronized {
      index.postSplit(io.split())
      splits.incrementAndGet()
    }
  }

  def compactCheck() {
    compactor.compactIfNeeded(minFileSizeToCompact, dataCompactThreshold)
  }

  def compact() {
    this.synchronized {
      compactor.forceCompact()
    }
  }

  def compactAll() {
    this.synchronized {
      compactor.compactActive()
      compactor.forceCompact()
    }
  }

  def keys() {
    notImplementedYet()
  }

  def values() {
    notImplementedYet()
  }

  def count() = index.size

  override def toString = "{name=%s, dir=%s}".format(name, dir)

  def jmxInit() {
    val server = ManagementFactory.getPlatformMBeanServer
    val beanName = new ObjectName("Bytecask_" + name + ":type=BytecaskBean")
    server.registerMBean(new BytecaskJmx(this), beanName)
  }

}

trait ValueProcessor {
  def before(b: Bytes): Bytes

  def after(b: Option[Bytes]): Option[Bytes]
}

@inline
object PassThru extends ValueProcessor {
  @inline def before(b: Bytes) = b

  @inline def after(b: Option[Bytes]) = b
}

@inline
object Compressor extends ValueProcessor {
  @inline def before(b: Bytes) = compress(b)

  @inline def after(b: Option[Bytes]) = Some(uncompress(b.get))
}

trait BytecaskJmxMBean {
  def stats(): String
}

class BytecaskJmx(db: Bytecask) extends BytecaskJmxMBean {
  def stats() = db.stats()
}


