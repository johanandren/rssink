package models

case class FeedDescriptor(feedType: FeedType, url: String) {
  val feedName = "feed-" + url.hashCode
}
