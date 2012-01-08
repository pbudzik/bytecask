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
* Date: 1/8/12
* Time: 3:37 PM
*/

package bytecask

import java.util.concurrent.{ConcurrentHashMap, TimeUnit, ConcurrentLinkedQueue, Semaphore}
import java.io.RandomAccessFile
import bytecask.Utils._

import collection.JavaConversions._

abstract class ResourcePool[T <: {def close()}](maxResources: Int) {

  private final val semaphore = new Semaphore(maxResources, true)

  private final val resources = new ConcurrentLinkedQueue[T]()

  def createResource(): T

  def get(maxWaitMillis: Long): T = {
    semaphore.tryAcquire(maxWaitMillis, TimeUnit.MILLISECONDS)
    var resource = resources.poll()
    if (resource == null) {
      try {
        resource = createResource()
      } catch {
        case e: Exception => throw new RuntimeException("Cannot create resource", e)
      }
      finally {
        semaphore.release()
      }
    }
    resource
  }

  def release(resource: T) {
    resources.add(resource)
    semaphore.release()
  }

  def destroy() {
    resources.foreach(_.close())
  }
}

abstract class FileReadersPool[T <: {def close()}](maxReaders: Int) {

  private val cache = new ConcurrentHashMap[String, ResourcePool[T]]()

  def get(file: String): T = {
    var pool = cache.get(file)
    if (pool == null) {
      pool = new ResourcePool[T](maxReaders) {
        def createResource() = createReader(file)
      }
      cache.put(file, pool)
    }
    pool.get(1000)
  }

  def release(file: String, reader: T) {
    cache.get(file).release(reader)
  }

  def createReader(file: String): T

  def destroy() {
    cache.foreach(_._2.destroy())
  }
}

class RandomAccessFilePool extends FileReadersPool[RandomAccessFile](processorsNum) {
  def createReader(file: String) = {
    new RandomAccessFile(file, "r")
  }

  override def get(file: String) = {
    val reader = super.get(file)
    reader.seek(0)
    reader
  }
}
