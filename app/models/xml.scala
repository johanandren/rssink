package models

import xml.NodeSeq

trait XmlReader[A] {

  def parse(node: NodeSeq): Either[RuntimeException, A]
}

trait XmlWriter[A] {
  def write(a: A): NodeSeq
}




