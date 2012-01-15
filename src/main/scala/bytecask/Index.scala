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
* Time: 6:05 PM
*/

package bytecask

import collection.mutable.Map
import java.io.File

import bytecask.Utils._

/*
Index (aka Keydir)- keeps position and length of entry in file
 */

final class Index(io: IO) extends Logging with Locking {

  private val index = Map[Bytes, IndexEntry]()

  def init() {
    ls(io.dir).toList.filter(_.length() > 0).foreach(indexFile(_))
  }

  private def indexFile(file: File) {
    IO.readEntries(file, addEntry)
    debug("after: " + index)
  }

  private def addEntry(file: File, entry: FileEntry) = writeLock {
    index.put(entry.key, IndexEntry(file.getName, entry.pos, entry.size, entry.timestamp))
  }

  def update(key: Bytes, pos: Int, length: Int, timestamp: Int) = writeLock {
    index.put(key, IndexEntry(IO.ACTIVE_FILE_NAME, pos, length, timestamp))
  }

  def get(key: Bytes) = readLock {
    index.get(key)
  }

  def delete(key: Bytes) = writeLock {
    index.remove(key)
  }

  def postSplit(file: String) {
    writeLock {
      for ((key, entry) <- index) {
        if (entry.isActive) {
          index.put(key, IndexEntry(file, entry.pos, entry.length, entry.timestamp))
        }
      }
    }
  }

  def hasEntry(entry: FileEntry) = {
    val e = index.get(entry.key)
    //debug("hasEntry: " + e + " -> " + entry)
    !e.isEmpty && e.get.timestamp == entry.timestamp
  }

  def contains(key: Bytes) = index.contains(key)

  def size = index.size

  def getIndex = index
}

case class IndexEntry(file: String, pos: Int, length: Int, timestamp: Int) {
  //FIXME: reduce size as it is kept in memory
  def isInactive = file != IO.ACTIVE_FILE_NAME

  def isActive = !isInactive
}
