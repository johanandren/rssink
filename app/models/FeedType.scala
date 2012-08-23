package models

abstract class FeedType {
  override def toString = getClass.getSimpleName.replaceAll("\\$","")
}
