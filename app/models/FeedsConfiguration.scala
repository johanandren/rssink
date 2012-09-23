package models

import models.FeedType

case class FeedsConfiguration(name: String, feeds: Map[String, List[(FeedType, String)]])
