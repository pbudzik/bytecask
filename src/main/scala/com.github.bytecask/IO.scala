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
* Time: 5:58 PM
*/

package com.github.bytecask

import java.util.concurrent.atomic.AtomicInteger
import java.io._

import com.github.bytecask.Utils._
import java.nio.ByteBuffer
import com.github.bytecask.Files.richReader
import java.util.zip.Adler32

object IO extends Logging {
  val HEADER_SIZE = 14 //crc, ts, ks, vs -> 4 + 4 + 2 + 4 bytes
  val DEFAULT_MAX_FILE_SIZE = Int.MaxValue // 2GB
  val ACTIVE_FILE_NAME = "0"
  val DATA_FILE_REGEX = "^[0-9]+$"

  def appendDataEntry(appender: RandomAccessFile, key: Bytes, value: Bytes) = {
    val pos = appender.getFilePointer
    val timestamp = (Utils.now / 1000).intValue()
    val keySize = key.size
    val valueSize = value.size
    val length = IO.HEADER_SIZE + keySize + valueSize
    val buffer = ByteBuffer.allocate(length)
    putInt32(buffer, timestamp, 4)
    putInt16(buffer, keySize, 8)
    putInt32(buffer, valueSize, 10)
    buffer.position(buffer.position() + 14)
    buffer.put(key)
    buffer.put(value)
    val crc = new Adler32
    crc.update(buffer.array(), 4, length - 4)
    putInt32(buffer, crc.getValue, 0)
    buffer.flip()
    appender.getChannel.write(buffer)
    (pos.toInt, length, timestamp)
  }

  def appendHintEntry(appender: RandomAccessFile, timestamp: Int, keySize: Int, valueSize: Int, pos: Int, key: Array[Byte]) {
    val buffer = ByteBuffer.allocate(4 + 2 + 4 + 4 + keySize)
    putInt32(buffer, timestamp, 0)
    putInt16(buffer, keySize, 4)
    putInt32(buffer, valueSize, 6)
    putInt32(buffer, pos, 10)
    buffer.position(buffer.position() + 14)
    buffer.put(key)
    buffer.flip()
    appender.getChannel.write(buffer)
  }

  /*
 Indexed read
  */

  def readDataEntry(reader: RandomAccessFile, entry: IndexEntry) = {
    reader.seek(entry.pos)
    val buffer = ByteBuffer.allocate(entry.length)
    val read = reader.getChannel.read(buffer)
    buffer.flip()
    if (read < entry.length) throw new IOException("Could not read all data: %s/%s".format(read, entry.length))
    val expectedCrc = readUInt32(buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3))
    val crc = new Adler32
    val a = buffer.array()
    crc.update(a, 4, entry.length - 4)
    val actualCrc = crc.getValue.toInt
    if (expectedCrc != actualCrc) throw new IOException("CRC check failed: %s != %s, entry: %s"
      .format(expectedCrc, actualCrc, entry))
    val timestamp = readUInt32(buffer.get(4), buffer.get(5), buffer.get(6), buffer.get(7))
    val keySize = readUInt16(buffer.get(8), buffer.get(9))
    val valueSize = readUInt32(buffer.get(10), buffer.get(11), buffer.get(12), buffer.get(13))
    val key = new Array[Byte](keySize)
    Array.copy(a, IO.HEADER_SIZE, key, 0, keySize)
    val value = new Array[Byte](valueSize)
    Array.copy(a, IO.HEADER_SIZE + keySize, value, 0, valueSize)
    DataEntry(entry.pos, actualCrc, keySize, valueSize, timestamp, key, value)
  }

  /*
 Iterative non-indexed data entry read
  */

  def readDataEntry(reader: RandomAccessFile) = {
    val pos = reader.getFilePointer
    val header = new Array[Byte](IO.HEADER_SIZE)
    reader.readOrThrow(header, "Failed to read chunk of %s bytes".format(IO.HEADER_SIZE))
    val expectedCrc = readUInt32(header(0), header(1), header(2), header(3))
    val timestamp = readUInt32(header(4), header(5), header(6), header(7))
    val keySize = readUInt16(header(8), header(9))
    val valueSize = readUInt32(header(10), header(11), header(12), header(13))
    val key = new Array[Byte](keySize)
    reader.readOrThrow(key, "Failed to read chunk of %s bytes".format(keySize))
    val value = new Array[Byte](valueSize)
    reader.readOrThrow(value, "Failed to read chunk of %s bytes".format(valueSize))
    val crc = new Adler32
    crc.update(header, 4, 10)
    crc.update(key, 0, keySize)
    crc.update(value, 0, valueSize)
    val actualCrc = crc.getValue.toInt
    if (expectedCrc != actualCrc) throw new IOException("CRC check failed: %s != %s".format(expectedCrc, actualCrc))
    DataEntry(pos.toInt, actualCrc, keySize, valueSize, timestamp, key, value)
  }

   /*
 Iterative non-indexed hint entry read
  */

  def readHintEntry(reader: RandomAccessFile) = {
    val header = ByteBuffer.allocate(14)
    reader.getChannel.read(header)
    header.flip()
    val timestamp = readUInt32(header.get(0), header.get(1), header.get(2), header.get(3))
    val keySize = readUInt16(header.get(4), header.get(5))
    val valueSize = readUInt32(header.get(6), header.get(7), header.get(8), header.get(9))
    val pos = readUInt32(header.get(10), header.get(11), header.get(12), header.get(13))
    val buffer = ByteBuffer.allocate(keySize)
    reader.getChannel.read(buffer)
    buffer.flip()
    val key = buffer.array()
    HintEntry(timestamp, keySize, valueSize, pos, key)
  }

  @inline
  def readDataEntry(pool: RandomAccessFilePool, dir: String, entry: IndexEntry): DataEntry = {
    withPooled(pool, dir + "/" + entry.file) {
      reader => readDataEntry(reader, entry)
    }
  }

  /*
  Used by index to restore index from files - either a hint file if exists
  or a data file
  */

  def readDataEntries(file: File, callback: (File, DataEntry) => Any): Boolean = {
    val length = file.length()
    val reader = new RandomAccessFile(file, "r")
    try {
      while (reader.getFilePointer < length) {
        val entry = readDataEntry(reader)
        callback(file, entry)
      }
      true
    } catch {
      case e: IOException =>
        warn(e.toString)
        false
    } finally {
      reader.close()
    }
  }

  def readHintEntries(file: File, callback: (File, HintEntry) => Any) = {
    val length = file.length()
    val reader = new RandomAccessFile(file, "r")
    try {
      while (reader.getFilePointer < length) {
        val entry = readHintEntry(reader)
        val path = file.getAbsolutePath
        callback(path.slice(0, path.length() - 1).mkFile, entry)
      }
      true
    } catch {
      case e: IOException =>
        warn(e.toString)
        false
    } finally {
      reader.close()
    }
  }

  @inline
  private def readUInt32(a: Byte, b: Byte, c: Byte, d: Byte) = {
    (a & 0xFF) << 24 | (b & 0xFF) << 16 | (c & 0xFF) << 8 | (d & 0xFF) << 0
  }

  @inline
  private def readUInt16(a: Byte, b: Byte) = (a & 0xFF) << 8 | (b & 0xFF) << 0

  @inline
  private def putInt32(buffer: ByteBuffer, value: Int, index: Int = 0) {
    buffer.put(index, (value >>> 24).toByte)
    buffer.put(index + 1, (value >>> 16).toByte)
    buffer.put(index + 2, (value >>> 8).toByte)
    buffer.put(index + 3, value.byteValue)
  }

  @inline
  private def putInt32(buffer: ByteBuffer, value: Long, index: Int) {
    buffer.put(index, (value >>> 24).toByte)
    buffer.put(index + 1, (value >>> 16).toByte)
    buffer.put(index + 2, (value >>> 8).toByte)
    buffer.put(index + 3, value.toByte)
  }

  @inline
  private def putInt16(buffer: ByteBuffer, value: Int, index: Int = 0) {
    buffer.put(index, (value >>> 8).toByte)
    buffer.put(index + 1, value.toByte)
  }
}

final class IO(val dir: String, maxConcurrentReaders: Int = 10) extends Closeable with Logging with Locking {
  val activeFile = dir + "/" + IO.ACTIVE_FILE_NAME
  val splits = new AtomicInteger
  var appender = createAppender()
  lazy val readers = new RandomAccessFilePool(maxConcurrentReaders)

  def appendDataEntry(key: Bytes, value: Bytes) = {
    IO.appendDataEntry(appender, key, value)
  }

  def readValue(entry: IndexEntry): Array[Byte] = {
    IO.readDataEntry(readers, dir, entry).value
  }

  private def createAppender() = writeLock {
    new RandomAccessFile(activeFile, "rw")
  }

  def split() = {
    //debug("Splitting...")
    appender.close()
    val next = nextFile()
    readers.invalidate(activeFile)
    activeFile.mkFile.renameTo(next)
    appender = createAppender()
    splits.incrementAndGet()
    next.getName
  }

  /*
  Next file that should be created to start appending there
   */

  private def nextFile() = {
    val files = ls(dir).filter(f => f.isFile && f.getName.matches(IO.DATA_FILE_REGEX)).map(_.getName.toInt).sortWith(_ < _)
    val slot = firstSlot(files)
    val next = if (!slot.isEmpty) slot.get else (files.last + 1)
    (dir / next).mkFile
  }

  def close() {
    readers.destroy()
    appender.close()
  }

  def pos = appender.getFilePointer

  /*
 Deletes, but also makes sure whenever a file is deleted we don't keep cached file objects
  */

  def delete(file: String): Boolean = delete(file.mkFile)

  def delete(file: File): Boolean = {
    if (file.delete()) {
      readers.invalidate(file.getAbsolutePath)
      true
    } else false
  }
}

final case class DataEntry(pos: Int, crc: Int, keySize: Int, valueSize: Int, timestamp: Int, key: Array[Byte], value: Array[Byte]) {
  def size = IO.HEADER_SIZE + keySize + valueSize
}

final case class HintEntry(timestamp: Int, keySize: Int, valueSize: Int, pos: Int, key: Array[Byte]) {
  def size = IO.HEADER_SIZE + keySize + valueSize
}