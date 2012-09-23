package actors

import models.atom._
import xml.NodeSeq
import models.{FeedsConfiguration, FeedDescriptor}

case object Init

case class SetupFeeds(feeds: FeedsConfiguration)
case class TearDownFeeds(feeds: FeedsConfiguration)
case object TearDownAllConfigurations

case object PerformUpdate
case class UpdateFeed(feed: FeedDescriptor)
case class FeedUpdate(feed: FeedDescriptor, atom: AtomFeed)

// HttpClientActor messages
case class FetchFeed(feed: FeedDescriptor) // fetch this feed
sealed abstract class FetchResult
case class FeedFetched(xml: NodeSeq) extends FetchResult // ok, here you are
case class FeedFetchFailed(why: String) extends FetchResult // ohnoez, failure

case object GetFeed
case class Feed(feed: AtomFeed)