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
Index (aka Keydir)- keeps position and length of entry in a file
 */

final class Index(io: IO) extends Logging with Locking with Tracking {

  private val index = Map[Bytes, IndexEntry]()

  def init() {
    debug("Initializing index...")
    if (ls(io.dir).toList.filter(_.length() > 0).map(indexFile(_)).filter(!_).size > 0) incErrors
  }

  private def hintFile(file: File) = (file.getAbsolutePath + "h").mkFile

  private def indexFile(file: File) = {
    if (hintFile(file).exists()) {
      debug("hint file exists for " + file)
      IO.readHintEntries(hintFile(file), processHintEntry)
    } else IO.readDataEntries(file, processDataEntry)
  }

  private def processDataEntry(file: File, entry: DataEntry) = writeLock {
    if (entry.valueSize == 0)
      index.remove(entry.key)
    else
      index.put(entry.key, IndexEntry(file.getName, entry.pos, entry.size, entry.timestamp))
  }

  private def processHintEntry(file: File, entry: HintEntry) = writeLock {
    if (entry.valueSize == 0)
      index.remove(entry.key)
    else
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

  def hasEntry(entry: DataEntry) = {
    val e = index.get(entry.key)
    //debug("hasEntry: " + e + " -> " + entry)
    !e.isEmpty && e.get.timestamp == entry.timestamp
  }

   def contains(key: Bytes) = index.contains(key)

  def size = index.size

  def keys = index.keys

  def getIndex = index
}

final case class IndexEntry(file: String, pos: Int, length: Int, timestamp: Int)
