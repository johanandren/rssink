package actors

import akka.actor.{ActorLogging, Actor}
import models.atom.{Id, AtomFeed}

object AggregateActor {
  // messages
  case object GetFeed
  case class Feed(feed: AtomFeed)

}

/** Holds one aggregate of multiple feeds, publishes updates to those feeds to listeners */
class AggregateActor(name: String) extends Actor with PersistedFeed with ActorLogging with EntryEventSource {

  import AggregateActor._
  import FeedActor._

  def feedName = "aggregate-" + name.hashCode

  private var feed: Option[AtomFeed] = None

  def receive = aggregateReceive orElse entryEventSourceReceive

  def aggregateReceive: Receive = {

    case FeedUpdated(updatedFeed, atom) => {
      log.debug("Got update from feed " + updatedFeed)
      feed = feed.map(_.aggregate(atom))
      log.debug("Aggregate updated (" + feed.map(_.entries.size).getOrElse(0) + " entries)")

      val updatedEntries = feed.map(_.entries).getOrElse(Seq())

      // publish updates to all listeners
      for (entry <- updatedEntries) publish(entry)
    }

    case GetFeed => sender ! Feed(feed.get)
    
  }

  override def preStart() {
    // load or create
    feed = loadFeed.orElse {
      Some(AtomFeed(
        id = Id(feedName.hashCode.toString),
        title = feedName,
        subtitle = "Aggregate",
        fetched = None,
        updated = None,
        feedUrl = "TODO",
        siteUrl = "TODO",
        entries = Seq()))

    }
  }

  override def postStop() {
    storeFeed(feed)
  }

}