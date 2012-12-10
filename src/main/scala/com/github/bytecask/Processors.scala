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
* Date: 12/10/12
* Time: 7:40 PM
*/

package com.github.bytecask

import com.github.bytecask.Utils._
import scala.Some

/**
 * Hook to install processing like compression
 */

trait ValueProcessor {
  def before(b: Bytes): Bytes

  def after(b: Option[Bytes]): Option[Bytes]
}

@inline
object PassThru extends ValueProcessor {
  @inline def before(b: Bytes) = b

  @inline def after(b: Option[Bytes]) = b
}

@inline
object Compressor extends ValueProcessor {
  @inline def before(b: Bytes) = compress(b)

  @inline def after(b: Option[Bytes]) = Some(uncompress(b.get))
}

