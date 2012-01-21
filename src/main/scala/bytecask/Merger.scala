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
* Time: 9:45 PM
*/

package bytecask

import collection.mutable.Map
import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}
import java.io.{RandomAccessFile, File}

/*
Merges inactive files to save space.
 */

final class Merger(io: IO, index: Index) extends Logging {
  val merges = new AtomicInteger
  val lastMerge = new AtomicLong
  val changes = Map[String, Delta]()

  import bytecask.Utils._

  def entryChanged(entry: IndexEntry) {
    changes.synchronized {
      val delta = changes.getOrElseUpdate(entry.file, Delta(0, entry.length))
      changes.put(entry.file, Delta(delta.entries + 1, delta.length + entry.length))
    }
  }

  def mergeIfNeeded(dataThreshold: Int) {
    debug("Checking changes: " + changes)
    val files = for (
      (file, delta) <- changes
      if (delta.length > dataThreshold) //might test number of entries altered
    ) yield file
    debug("Files to be merged: " + collToString(files))
    if (files.size > 1)
      merge(files)
  }

  /**
   * Merges files and creates a "hint" file out of the compacted data file
   */

  private def merge(files: Iterable[String]) = {
    if (files.size > 1) {
      val target = files.head
      debug("Merging files: %s -> '%s'".format(collToString(files), target))
      val tmp = temporaryFile(target)
      val subIndex = Map[Bytes, IndexEntry]()
      withResource(new RandomAccessFile(tmp, "rw")) {
        appender =>
          files.foreach {
            file => IO.readDataEntries(dbFile(file), (file: File, entry: DataEntry) => {
              if (entry.valueSize > 0 && index.hasEntry(entry)) {
                val (pos, length, timestamp) = IO.appendDataEntry(appender, entry.key, entry.value)
                val indexEntry = IndexEntry(file.getName, pos, length, timestamp)
                subIndex.put(entry.key, indexEntry)

              }
            })
          }
      }
      withResource(new RandomAccessFile(hintFile(target), "rw")) {
        appender =>
          if (!subIndex.isEmpty)
            index.synchronized {
              for ((key, indexEntry) <- subIndex) {
                index.getIndex.put(key, indexEntry)
                IO.appendHintEntry(appender, indexEntry.timestamp, key.length, indexEntry.length, indexEntry.pos, key)
              }
              files.foreach(changes.remove(_))
              files.foreach(file => io.delete(dbFile(file)))
              tmp.renameTo(dbFile(target))
              io.delete(tmp)
              lastMerge.set(now)
              merges.incrementAndGet()
            }
      }
    }
  }

  def forceMerge() {
    merge(ls(io.dir).map(_.getName).filter(_ != IO.ACTIVE_FILE_NAME).map(_.toInt).sortWith(_ < _).map(_.toString))
  }

  private def temporaryFile(file: String) = (io.dir + "/" + file + "_").mkFile

  private def hintFile(file: String) = (io.dir + "/" + file + "h").mkFile

  private def dbFile(file: String) = (io.dir + "/" + file).mkFile
}

/*
Represents "change measure" for a file - how many entries / how much space
is to be potentially regained
 */

case class Delta(entries: Int, length: Int)