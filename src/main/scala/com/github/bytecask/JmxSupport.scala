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
* Date: 12/8/12
* Time: 11:04 AM
*/

package com.github.bytecask

import management.ManagementFactory
import javax.management.ObjectName

trait JmxSupport {

  val name: String

  val bytecask: Bytecask

  val beanName = new ObjectName("Bytecask_" + name + ":type=BytecaskBean")

  lazy val server = ManagementFactory.getPlatformMBeanServer

  def jmxInit() {
    server.registerMBean(new BytecaskJmx(bytecask), beanName)
  }

  def jmxDestroy() {
    server.unregisterMBean(beanName)
  }
}

trait BytecaskJmxMBean {
  def stats(): String
}

class BytecaskJmx(db: Bytecask) extends BytecaskJmxMBean {
  def stats() = db.stats()
}

