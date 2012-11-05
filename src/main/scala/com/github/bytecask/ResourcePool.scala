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

package com.github.bytecask

import java.util.concurrent.{ConcurrentHashMap, TimeUnit, ConcurrentLinkedQueue, Semaphore}
import java.io.RandomAccessFile

import collection.JavaConversions._

/**
 * Instead of creating a RandomAccessFile per each reading thread the idea
 * is to keep a pool of readers per each file and reuse it keeping it open.
 * This avoids costly file handlers creation to the same files.
 */

abstract class ResourcePool[T <: {def close()}](maxResources: Int) {

  private final val semaphore = new Semaphore(maxResources, true)

  private final val resources = new ConcurrentLinkedQueue[T]()

  def createResource(): T

  def get(maxWaitMillis: Long): T = {
    semaphore.tryAcquire(maxWaitMillis, TimeUnit.MILLISECONDS)
    resources.poll() match {
      case r: T => r
      case _ => {
        try {
          createResource()
        } catch {
          case e: Exception => throw new RuntimeException("Cannot create resource", e)
        }
        finally {
          semaphore.release()
        }
      }
    }
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
    val pool = cache.get(file) match {
      case pool: ResourcePool[T] => pool
      case _ => {
        val pool = new ResourcePool[T](maxReaders) {
          def createResource() = createReader(file)
        }
        cache.put(file, pool)
        pool
      }
    }
    pool.get(1000)
  }

  def release(file: String, reader: T) {
    cache.get(file).release(reader)
  }

  def createReader(file: String): T

  def invalidate(file: String) {
    cache.get(file) match {
      case pool: ResourcePool[T] => pool.destroy()
      case _ => cache.remove(file)
    }
  }

  def destroy() {
    cache.foreach(_._2.destroy())
  }
}

/**
 * TODO: if there happens to be many files the file OS limit may be exceeded
 * Might be a LRU cache w/ open files closure
 */

class RandomAccessFilePool(maxFiles: Int) extends FileReadersPool[RandomAccessFile](maxFiles) {
  def createReader(file: String) = new RandomAccessFile(file, "r")

  override def get(file: String) = {
    super.get(file) match {
      case reader if !reader.getChannel.isOpen => {
        invalidate(file)
        get(file)
      }
      case reader => reader
    }
  }
}
