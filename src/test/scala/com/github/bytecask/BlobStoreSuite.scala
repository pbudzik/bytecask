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
* Date: 7/2/11
* Time: 12:07 PM
*/

package com.github.bytecask

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import com.github.bytecask.Utils._
import com.github.bytecask.Files._
import java.io.{FileInputStream, FileOutputStream}

class BlobStoreSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  test("store and retrieve") {
    val db = new Bytecask(mkTempDir) with BlobStore {
      val blockSize = 1024 * 1024
    }

    val file = "/tmp/blob"
    withResource(new FileOutputStream(file)) {
      os =>
        val buf = new Array[Byte](1024)
        for (i <- 0 to 11000) {
          scala.util.Random.nextBytes(buf)
          os.write(buf)
        }
    }

    db.storeBlob("blobby", new FileInputStream(file))

    db.count() should be(12) //11 chunks + 1 descriptor

    val file2 = "/tmp/blob2"
    withResource(new FileOutputStream(file2)) {
      os => db.retrieveBlob("blobby", os)
    }

    file.mkFile.length() should be(file2.mkFile.length())

    db.destroy()

    file.mkFile.delete()
    file2.mkFile.delete()
  }

}