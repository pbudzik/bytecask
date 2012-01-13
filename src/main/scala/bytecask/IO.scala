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

package bytecask

import java.util.zip.CRC32
import java.util.concurrent.atomic.AtomicInteger
import java.io._

import bytecask.Utils._

object IO {
  val HEADER_SIZE = 14 // 4 + 4 + 2 + 4 bytes
  val DEFAULT_MAX_FILE_SIZE = Int.MaxValue // 2GB
  val ACTIVE_FILE_NAME = "0"
  val FILE_REGEX = "^[0-9]+$"

  def appendEntry(appender: RandomAccessFile, key: Bytes, value: Bytes) = appender.synchronized {
    val pos = appender.getFilePointer.toInt
    val timestamp = (Utils.now / 1000).intValue()
    val keySize = key.size
    val valueSize = value.size
    val length = IO.HEADER_SIZE + keySize + valueSize
    val buffer = new Array[Byte](length)
    writeInt32(timestamp, buffer, 4)
    writeInt16(keySize, buffer, 8)
    writeInt32(valueSize, buffer, 10)
    key.bytes.copyToArray(buffer, 14)
    value.bytes.copyToArray(buffer, 14 + keySize)
    val crc = new CRC32
    crc.update(buffer, 4, length - 4)
    val crcValue = crc.getValue
    writeInt32(crcValue, buffer, 0)
    appender.write(buffer, 0, length)
    (pos, length, timestamp)
  }

  def readEntry(reader: RandomAccessFile, entry: IndexEntry) = {
    reader.seek(entry.pos)
    val buffer = new Array[Byte](entry.length)
    val read = reader.read(buffer)
    if (read < entry.length) throw new IOException("Could not read all data: %s/%s".format(read, entry.length))
    val expectedCrc = readUInt32(buffer(0), buffer(1), buffer(2), buffer(3))
    val crc = new CRC32
    crc.update(buffer, 4, entry.length - 4)
    val actualCrc = crc.getValue.toInt
    if (expectedCrc != actualCrc) throw new IOException("CRC check failed: %s != %s".format(expectedCrc, actualCrc))
    val timestamp = readUInt32(buffer(4), buffer(5), buffer(6), buffer(7))
    val keySize = readUInt16(buffer(8), buffer(9))
    val valueSize = readUInt32(buffer(10), buffer(11), buffer(12), buffer(13))
    val key = new Array[Byte](keySize)
    Array.copy(buffer, IO.HEADER_SIZE, key, 0, keySize)
    val value = new Array[Byte](valueSize)
    Array.copy(buffer, IO.HEADER_SIZE + keySize, value, 0, valueSize)
    FileEntry(entry.pos, actualCrc, keySize, valueSize, timestamp, key, value)
  }

  def readEntry(reader: RandomAccessFile) = {
    val pos = reader.getFilePointer
    val crcBuffer = new Array[Byte](4)
    val read = reader.read(crcBuffer)
    if (read <= 0) throw new IOException("Nothing more to read")
    val expectedCrc = readUInt32(crcBuffer(0), crcBuffer(1), crcBuffer(2), crcBuffer(3))
    val tsBuffer = new Array[Byte](4)
    reader.read(tsBuffer)
    val timestamp = readUInt32(tsBuffer(0), tsBuffer(1), tsBuffer(2), tsBuffer(3))
    val ksBuffer = new Array[Byte](2)
    reader.read(ksBuffer)
    val keySize = readUInt16(ksBuffer(0), ksBuffer(1))
    val vsBuffer = new Array[Byte](4)
    reader.read(vsBuffer)
    val valueSize = readUInt32(vsBuffer(0), vsBuffer(1), vsBuffer(2), vsBuffer(3))
    val key = new Array[Byte](keySize)
    reader.read(key)
    val value = new Array[Byte](valueSize)
    reader.read(value)
    val tested = tsBuffer ++ ksBuffer ++ vsBuffer ++ key ++ value //FIXME
    val crc = new CRC32
    crc.update(tested, 0, tested.length)
    val actualCrc = crc.getValue.toInt
    if (expectedCrc != actualCrc) throw new IOException("CRC check failed: %s != %s".format(expectedCrc, actualCrc))
    FileEntry(pos.toInt, actualCrc, keySize, valueSize, timestamp, key, value)
  }

  @inline
  def readEntry(dir: String, entry: IndexEntry): FileEntry = {
    withResource(new RandomAccessFile(dir + "/" + entry.file, "r")) {
      reader => readEntry(reader, entry)
    }
  }

  def readEntries(file: File, callback: (File, FileEntry) => Any) {
    val reader = new RandomAccessFile(file, "r")
    try {
      readAll(file, reader, callback)
    } catch {
      case e: IOException => reader.close()
    }
  }

  private def readAll(file: File, reader: RandomAccessFile, callback: (File, FileEntry) => Any) {
    val entry = readEntry(reader)
    callback(file, entry)
    readAll(file, reader, callback)
  }

  @inline
  private def readUInt32(a: Byte, b: Byte, c: Byte, d: Byte) = {
    (a & 0xFF) << 24 | (b & 0xFF) << 16 | (c & 0xFF) << 8 | (d & 0xFF) << 0
  }

  @inline
  private def readUInt16(a: Byte, b: Byte) = (a & 0xFF) << 8 | (b & 0xFF) << 0

  @inline
  private def writeInt32(value: Int, buffer: Array[Byte], start: Int) {
    buffer.update(start, (value >>> 24).toByte)
    buffer.update(start + 1, (value >>> 16).toByte)
    buffer.update(start + 2, (value >>> 8).toByte)
    buffer.update(start + 3, value.byteValue)
  }

  @inline
  private def writeInt32(value: Long, buffer: Array[Byte], start: Int) {
    buffer.update(start, (value >>> 24).toByte)
    buffer.update(start + 1, (value >>> 16).toByte)
    buffer.update(start + 2, (value >>> 8).toByte)
    buffer.update(start + 3, value.toByte)
  }

  @inline
  private def writeInt16(value: Int, buffer: Array[Byte], start: Int) {
    buffer.update(start, (value >>> 8).toByte)
    buffer.update(start + 1, value.toByte)
  }
}

final class IO(val dir: String) extends Closeable with Logging with Locking {
  val activeFile = dir + "/" + IO.ACTIVE_FILE_NAME
  var appender = createAppender()
  val splits = new AtomicInteger

  def appendEntry(key: Bytes, value: Bytes) = {
    IO.appendEntry(appender, key, value)
  }

  def readValue(entry: IndexEntry): Array[Byte] = {
    IO.readEntry(dir, entry).value
  }

  private def createAppender() = writeLock {
    new RandomAccessFile(activeFile, "rw")
  }

  def split() = {
    //debug("Splitting...")
    appender.close()
    val next = nextFile()
    activeFile.mkFile.renameTo(next)
    appender = createAppender()
    splits.incrementAndGet()
    next.getName
  }

  private def nextFile() = {
    val files = ls(dir).filter(f => f.isFile && f.getName.matches(IO.FILE_REGEX)).map(_.getName.toInt).sortWith(_ < _)
    val next = files.last + 1
    (dir / next).mkFile
  }

  def close() {
    appender.close()
  }

  def pos = appender.getFilePointer
}

final case class FileEntry(pos: Int, crc: Int, keySize: Int, valueSize: Int, timestamp: Int, key: Array[Byte], value: Array[Byte]) {
  def size = IO.HEADER_SIZE + key.length + value.length
}
