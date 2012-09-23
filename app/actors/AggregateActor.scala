package actors

import akka.actor.{PoisonPill, Actor}
import models.atom.{Id, AtomFeed}
import play.api.Logger

/** Holds one aggregate of multiple feeds
  */
class AggregateActor(private val name: String)
  extends Actor
  with PersistedFeed
  with PlayActorLogging {

  def feedName = "aggregate-" + name.hashCode

  private var feed: Option[AtomFeed] = None

  def receive = {
    case FeedUpdate(updatedFeed, atom) => {
      log.debug("Got update from feed " + updatedFeed)
      feed = feed.map(_.aggregate(atom))
      log.debug("Aggregate updated")
    }

    case GetFeed => sender ! Feed(feed.get)

    case x => log.warn("Unknown message: " + x)
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