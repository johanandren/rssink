package models

case class FeedsConfiguration(name: String, feeds: Map[String, List[(FeedType, String)]])
