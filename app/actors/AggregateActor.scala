package actors

import akka.actor.{ActorLogging, Actor}
import models.atom.{Id, AtomFeed}
import actors.EntryEventSource.ListenerRegistered

object AggregateActor {
  // messages
  case object GetFeed
  case class Feed(feed: AtomFeed)
  case object GetSummary
  case class Summary(title: String, subtitle: String)
}

/** Holds one aggregate of multiple feeds, publishes updates to those feeds to listeners */
class AggregateActor(name: String) extends Actor with PersistedFeed with ActorLogging with EntryEventSource {

  import AggregateActor._
  import FeedActor._

  def feedName = "aggregate-" + name.hashCode

  private var feed: AtomFeed = AtomFeed(Id(feedName), name, "Aggregate", None, None, "", "", Seq())

  def receive = aggregateReceive orElse entryEventSourceReceive

  def aggregateReceive: Receive = {

    case FeedUpdated(updatedFeed, atom) =>
      log.debug("Got update from feed " + updatedFeed)
      val originalEntries = feed.entries.toSet
      feed = feed.aggregate(atom)
      log.debug("Aggregate updated (" + feed.entries.size + " entries)")

      val updatedEntries = feed.entries.filterNot(originalEntries(_))

      // publish updates to all listeners
      for (entry <- updatedEntries) publishToAll(entry)

    case ListenerRegistered(listener) =>
      log.debug("Sending all entries " + feed.entries.size + " to new listener")
      // publish all entries at registration
      for (entry <- feed.entries) publish(entry, listener)

    case GetFeed => sender ! Feed(feed)

    case GetSummary => sender ! Summary(feed.title, feed.subtitle)

  }

  override def preStart() {
    // load or keep existing
    loadFeed.foreach(feed = _)
  }

  override def postStop() {
    storeFeed(Some(feed))
  }

}