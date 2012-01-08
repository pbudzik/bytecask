/*
* Copyright 2011
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
* Date: 4/10/11
* Time: 6:05 PM
*/

package bytecask

import org.slf4j.LoggerFactory

trait Logging {

  final def trace_? = logger.isTraceEnabled

  final def debug_? = logger.isDebugEnabled

  final def info_? = logger.isInfoEnabled

  final def warning_? = logger.isWarnEnabled

  final def error_? = logger.isErrorEnabled

  private lazy val logger = LoggerFactory.getLogger(this.getClass.getName)

  final def debug(fmt: => String, arg: Any, argN: Any*) {
    debug(message(fmt, arg, argN: _*))
  }

  final def debug(msg: => String) {
    if (debug_?) logger debug msg
  }

  final def info(msg: => String) {
    if (info_?) logger info msg
  }

  final def warn(msg: => String) {
    if (warning_?) logger warn msg
  }

  final def error(msg: => String) {
    if (error_?) logger error msg
  }

  protected final def message(fmt: String, arg: Any, argN: Any*): String = {
    if ((argN eq null) || argN.isEmpty) fmt.format(arg)
    else fmt.format((arg +: argN): _*)
  }
}