package actors

import models.atom._
import xml.NodeSeq
import models.FeedDescriptor

case object PerformUpdate
case class UpdateFeed(feed: FeedDescriptor)
case class FeedUpdate(feed: FeedDescriptor, atom: AtomFeed)

case class FetchFeed(feed: FeedDescriptor)
case class FeedFetched(xml: NodeSeq)

case object GetFeed
case class Feed(feed: AtomFeed)