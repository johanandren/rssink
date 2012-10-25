package actors

import akka.actor._
import akka.util.duration._
import models._
import models.atom._
import actors.HttpClientActor.{HttpResponseBody, FetchHttpUrl}

object FeedActor {
  // messages
  case object Init
  case object PerformUpdate
  case class FeedUpdated(descriptor: FeedDescriptor, feed: AtomFeed)

  def apply(feedUrl: String, feedType: FeedType, httpClient: ActorRef, aggregate: ActorRef): Actor = {
    val descriptor = FeedDescriptor(feedType, feedUrl)
    val minUpdateInterval = 1000 * 60 * 8
    new FeedActor(descriptor, minUpdateInterval, httpClient, aggregate)
  }

}

/**
 * Accepts a PerformUpdate request that will (possibly) make it update the feed contents
 * and if successful (and changed) send the updated feed to the given aggregate.
 */
class FeedActor(descriptor: FeedDescriptor, updateInterval: Long, httpClient: ActorRef, aggregate: ActorRef)
  extends Actor
  with PersistedFeed
  with ActorLogging {

  import FeedActor._
  import FeedParser._

  val ticker = context.system.scheduler.schedule(1 second, 10 minutes, self, PerformUpdate)

  def feedName = descriptor.feedName

  var feed: Option[AtomFeed] = None

  var feedParser: ActorRef = context.system.deadLetters

  override def preStart() {
    feed = loadFeed
    feedParser = context.actorOf(Props[FeedParser])
  }

  def receive = {

    case Init =>


    case PerformUpdate =>
      log.info("Request to update feed " + feedName)
      if (shouldBeUpdated(feed)) {
        httpClient ! FetchHttpUrl(descriptor.url)
      }

    case HttpResponseBody(body) => feedParser ! ParseFeed(descriptor, body)

    case FeedParsed(descriptor, newFeed) =>
      feed = feed.map(oldFeed => oldFeed.combine(newFeed)).orElse(Some(newFeed))

      // TODO only send to aggregate if changed
      log.debug("Feed state updated")

      aggregate ! FeedUpdated(descriptor, feed.get)

    case x => log.error("Unknown message of type " + x.getClass().toString + ": " + x + ", from " + sender)

  }

  def shouldBeUpdated(feed: Option[AtomFeed]) =
    feed.isEmpty ||
    feed.get.fetched.isEmpty ||
    System.currentTimeMillis > (feed.get.fetched.get.getTime + updateInterval)

  override def postStop() {
    storeFeed(feed)
    // Prevents the scheduler from being scheduled more than once (in case of restart of this actor)
    ticker.cancel()
  }

}

