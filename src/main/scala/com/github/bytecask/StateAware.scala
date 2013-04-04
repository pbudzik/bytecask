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
* Date: 4/4/13
* Time: 6:15 PM
*/

package com.github.bytecask

import java.util.concurrent.atomic.AtomicReference

object State extends Enumeration {
  type State = Value
  val Unmodified, AfterPut, AfterDelete, AfterPutAndDelete = Value
}

import com.github.bytecask.State._

trait StateAware {

  val bytecask: Bytecask

  val state = new AtomicReference[State](Unmodified)

  def getState = state.get

  def isDirty = state.get != Unmodified

  def resetState() {
    state.set(Unmodified)
  }

  def putDone() {
    val newState = state.get match {
      case Unmodified => AfterPut
      case AfterDelete => AfterPutAndDelete
      case _ => state.get
    }
    state.set(newState)
  }

  def deleteDone() {
    val newState = state.get match {
      case Unmodified => AfterDelete
      case AfterPut => AfterPutAndDelete
      case _ => state.get
    }
    state.set(newState)
  }

}

