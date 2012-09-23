package actors

import xml.NodeSeq
import akka.actor._
import akka.util.duration._
import models._
import models.atom._
import models.rss._
import FeedActor._

/**
 * Accepts a PerformUpdate request that will (possibly) make it update the feed contents
 * and if successful (and changed) send the updated feed to the given aggregate.
 */
class FeedActor(
    descriptor: FeedDescriptor,
    parser: Parser,
    updateInterval: Long,
    httpClient: ActorRef,
    aggregate: ActorRef)
  extends Actor
  with PersistedFeed
  with PlayActorLogging {

  val scheduler = context.system.scheduler.schedule(1 second, 10 minutes, self, PerformUpdate)

  def feedName = descriptor.feedName

  var feed: Option[AtomFeed] = None

  def receive = {
    case PerformUpdate => {
      log.debug("Request to update")

      val shouldBeUpdated =
        feed.isEmpty ||
        feed.get.fetched.isEmpty ||
        System.currentTimeMillis > (feed.get.fetched.get.getTime + updateInterval)

      if (shouldBeUpdated) {
        httpClient ! FetchFeed(descriptor)
      }
    }

    case FeedFetched(xml) => {
      log.debug("Got updated feed xml")
      val newFeed = parser(xml)

      newFeed.fold(
        error => log.warn("Failed to parse feed contents for: " + descriptor, error),
        success => {
          feed = feed match {
            case Some(oldFeed) => Some(oldFeed.combine(success))
            case None => Some(success)
          }

          // TODO only send to aggregate if changed
          log.debug("Feed state updated")

          aggregate ! FeedUpdate(descriptor, feed.get)
        }
      )
    }

    // TODO implement backoff
    case FeedFetchFailed(why) => log.info("Failed to fetch feed " + feed + ": " + why)

    case x => log.error("Unknown message of type " + x.getClass.toString + ": " + x + ", from " + sender)
  }

  override def preStart() {
    feed = loadFeed
  }

  override def postStop() {
    storeFeed(feed)
    // Prevents the scheduler from being scheduled more than once (in case of restart of this actor)
    scheduler.cancel()
  }

}

object FeedActor {

  /** feed parser, parses XML NodeSeq into AtomFeed */
  type Parser = NodeSeq => Either[RuntimeException, AtomFeed]

  val rssParser: Parser = xml => RssXmlReader.parse(xml).fold(
    error => Left(error),
    success => Right(RssAtomAdapter.convert(success))
  )

  val atomParser: Parser = AtomXmlFormat.parse(_)

  def apply(feedUrl: String, feedType: FeedType, httpClient: ActorRef, aggregate: ActorRef): Actor = {
    val descriptor = FeedDescriptor(feedType, feedUrl)
    val parser =
    feedType match {
      case Rss => rssParser
      case Atom => atomParser
    }
    val minUpdateInterval = 1000 * 60 * 8
    new FeedActor(descriptor, parser, minUpdateInterval, httpClient, aggregate)
  }
}