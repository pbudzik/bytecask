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
* Date: 1/11/12
* Time: 9:00 PM
*/

package com.github.bytecask

import java.util.concurrent.locks.ReentrantReadWriteLock

trait Locking {
  val lock = new ReentrantReadWriteLock()
  val read = lock.readLock()
  val write = lock.writeLock()

  def readLock[T](f: => T): T = {
    read.lock()
    try {
      f
    }
    finally {
      read.unlock()
    }
  }

  def writeLock[T](f: => T): T = {
    write.lock()
    try {
      f
    }
    finally {
      write.unlock()
    }
  }
}