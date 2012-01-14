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
Represents change measure for a file - how many entries and how much data
is to be potentially compacted
 */

case class Delta(entries: Int, length: Int)

/*
Compacts inactive files to save space. TODO: merging small files
 */

class Compactor(io: IO, index: Index) extends Logging {
  val compactions = new AtomicInteger
  val lastCompaction = new AtomicLong
  val changes = Map[String, Delta]()

  import bytecask.Utils._

  def entryChanged(entry: IndexEntry) {
    changes.synchronized {
      val delta = changes.getOrElseUpdate(entry.file, Delta(0, entry.length))
      changes.put(entry.file, Delta(delta.entries + 1, delta.length + entry.length))
    }
  }

  def compactIfNeeded(minFileSize: Int, dataThreshold: Int) {
    debug("Checking changes: " + changes)
    val files = for (
      (file, delta) <- changes
      if (dbFile(file).length() > minFileSize) && (delta.length > dataThreshold)
    ) yield file
    debug("Files to be compacted: " + collToString(files))
    if (files.size > 1)
      compact(files)
  }

  private def compact(files: Iterable[String]) {
    files.foreach(compactFile(_))
    lastCompaction.set(now)
    compactions.incrementAndGet()
  }

  private def compactFile(file: String) = {
    debug("Compacting '%s'".format(file))
    val subIndex = Map[Bytes, IndexEntry]()
    val tmp = temporaryFor(file)
    var written = false
    withResource(new RandomAccessFile(tmp, "rw")) {
      appender =>
        IO.readEntries(dbFile(file), (file: File, entry: FileEntry) => {
          //debug("entry: " + entry)
          if (entry.valueSize > 0 && index.hasEntry(entry)) {
            val (pos, length, timestamp) = IO.appendEntry(appender, entry.key, entry.value)
            subIndex.put(entry.key, IndexEntry(file.getName, pos, length, timestamp))
            written = true
          }
        })
    }
    if (written) {
      if (!subIndex.isEmpty)
        index.synchronized {
          //debug("Merging indices..." + file + " and " + tmp)
          for ((k, v) <- subIndex) index.getIndex.put(k, v)
          changes.remove(file)
          if (dbFile(file).delete()) {
            if (!tmp.renameTo(dbFile(file)))
              warn("Unable to rename: " + dbFile(file))
          } else warn("Unable to delete: " + dbFile(file))
        } else tmp.delete()
    } else {
      //after compaction the file is empty
      if (!tmp.delete()) warn("Unable to delete: " + tmp)
      if (!dbFile(file).delete()) warn("Unable to delete: " + dbFile(file))
    }
  }

  def compactActiveFile() {
    index.synchronized {
      compact(List(IO.ACTIVE_FILE_NAME))
    }
  }

  def forceCompact() {
    compact(ls(io.dir).map(_.getName))
  }

  private def temporaryFor(file: String) = (io.dir + "/" + file + "_").mkFile

  private def dbFile(file: String) = (io.dir + "/" + file).mkFile
}