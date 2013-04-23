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
* Date: 5/3/12
* Time: 10:55 AM
*/

package com.github.bytecask

import util.control.Breaks._
import collection.mutable.{Queue, ArrayBuffer}
import annotation.tailrec

/*
 * Scala implementation of Radix Tree: http://en.wikipedia.org/wiki/Radix_tree
 * It is not very much idiomatic as my functional skills still suck :)
 * It is also not optimized yet. Might be derecursivated.
 */

class RadixTree[T] {
  val root = new RadixTreeNode[T]()
  var size = 0

  def insert(key: String, value: T) {
    insert(key, root, value)
    size += 1
  }

  def find(key: String): Option[T] = {
    val visitor = new Visitor[T, T]() {
      def accept(key: String, parent: RadixTreeNode[T], node: RadixTreeNode[T]) {
        if (!node.isVirtual)
          result = node.value
      }
    }
    visit(key, visitor)
    visitor.result
  }

  def delete(key: String) = {
    val visitor = new Visitor[T, Boolean](Some(false)) {
      def accept(key: String, parent: RadixTreeNode[T], node: RadixTreeNode[T]) {
        result = Some(!node.isVirtual)
        if (result.get && node.children.size == 0) {
          val survivors = parent.children.filter(_.key != node.key)
          parent.children = survivors
          if (parent.children.size == 1 && parent.isVirtual)
            mergeNodes(parent, parent.children(0))
        } else if (node.children.size == 1) {
          mergeNodes(node, node.children(0))
        } else node.setVirtual()
        @inline
        def mergeNodes(parent: RadixTreeNode[T], child: RadixTreeNode[T]) {
          parent.key = parent.key + child.key
          parent.value = child.value
          parent.children = child.children
        }
      }
    }
    visit(key, visitor)

    if (visitor.result.get) size -= 1

    visitor.result.get
  }

  private def insert(key: String, node: RadixTreeNode[T], value: T) {
    val matched = node.longestCommonPrefix(key)
    if (node.isRoot || matched == 0 || matched < key.length && matched >= node.key.length) {
      val newKey = key.substring(matched, key.length)
      var inserted = false
      breakable {
        node.children.foreach {
          child => {
            if (child.key.startsWith(newKey.head.toString)) {
              inserted = true
              insert(newKey, child, value)
              break()
            }
          }
        }
      }
      if (!inserted) {
        val n0 = new RadixTreeNode[T](newKey, Some(value))
        node.children.append(n0)
      }
    } else if (matched == key.length() && matched == node.key.length()) {
      node.value = Some(value)
    } else if (matched > 0 && matched < node.key.length()) {
      val n1 = new RadixTreeNode[T](node.key.substring(matched, node.key.length), node.value, node.children.clone())
      node.key = key.substring(0, matched)
      node.setVirtual()
      node.children = ArrayBuffer(n1)
      if (matched < key.length) {
        val n2 = new RadixTreeNode[T](key.substring(matched, key.length()), Some(value))
        node.children.append(n2)
      } else node.value = Some(value)
    }
    else {
      val newKey = node.key.substring(matched, node.key.length())
      val child = new RadixTreeNode[T](newKey, node.value, node.children.clone())
      node.key = key
      node.value = Some(value)
      node.children.append(child)
    }
  }

  @inline
  private def visit[R](key: String, visitor: Visitor[T, R]) {
    visit(key, visitor, null, root)
  }

  private def visit[R](prefix: String, visitor: Visitor[T, R], parent: RadixTreeNode[T], node: RadixTreeNode[T]) {
    val matched = node.longestCommonPrefix(prefix)
    if (matched == prefix.length() && matched == node.key.length()) {
      visitor.accept(prefix, parent, node)
    } else if (node.isRoot || (matched < prefix.length() && matched >= node.key.length())) {
      val prefix2 = prefix.substring(matched, prefix.length())
      breakable {
        node.children.foreach {
          child => {
            if (child.key.startsWith(prefix2.head.toString)) {
              visit(prefix2, visitor, node, child)
              break()
            }
          }
        }
      }
    }
  }

  def getSize = size

  def getTotalKeysSize: Long = {
    var size = 0L
    val queue = Queue[RadixTreeNode[T]]()
    queue.enqueue(root)
    while (queue.nonEmpty) {
      val node = queue.dequeue()
      size += node.key.size
      node.children.foreach(queue.enqueue(_))
    }
    size
  }

  override def toString = {
    val queue = Queue[(RadixTreeNode[T], String)]()
    queue.enqueue((root, ""))
    val sb = new StringBuilder
    while (queue.nonEmpty) {
      val (node, key) = queue.dequeue()
      if (!node.isVirtual)
        sb.append(key + node.key).append(" -> ").append(node.value).append("\n")
      node.children.foreach(child => queue.enqueue((child, key + node.key)))
    }
    sb.toString()
  }

  def iterator = new Iterator[(String, T)] {
    val queue = Queue[(RadixTreeNode[T], String)]()
    queue.enqueue((root, ""))

    def next() = {
      @inline
      @tailrec
      def walk: (String, T) = {
        val (node, key) = queue.dequeue()
        node.children.foreach(child => queue.enqueue((child, key + node.key)))
        if (node.isVirtual)
          walk
        else
          (key + node.key, node.value.get)
      }
      walk
    }

    def hasNext = if (getSize > 0) !queue.isEmpty else false
  }

}

class RadixTreeNode[T](var key: String = "", var value: Option[T] = None, var children: ArrayBuffer[RadixTreeNode[T]] = ArrayBuffer[RadixTreeNode[T]]()) {

  def isVirtual = value.isEmpty

  def isRoot = (key == "")

  def setVirtual() {
    value = None
  }

  def longestCommonPrefix(prefix: String) = (prefix, key).zipped.takeWhile(Function.tupled(_ == _)).size

  override def toString = key

}

abstract class Visitor[T, R](var result: Option[R] = None) {
  def accept(key: String, parent: RadixTreeNode[T], node: RadixTreeNode[T])
}
