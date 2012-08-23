package actors

import xml.NodeSeq
import akka.actor.{ActorRef, Actor}
import play.api.Logger
import models._
import models.atom._
import models.rss._
import FeedActor._

class FeedActor(
    descriptor: FeedDescriptor,
    parser: Parser,
    httpClient: ActorRef,
    aggregate: ActorRef)
  extends Actor
  with PersistedFeed {

  def feedName = "feed-" + descriptor.url.hashCode

  val log = Logger("actor.feed")

  var feed: Option[AtomFeed] = None

  def receive = {
    case PerformUpdate => {
      log.debug("Request to update")
      httpClient ! FetchFeed(descriptor)
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
          log.debug("Feed state updated")

          aggregate ! FeedUpdate(descriptor, feed.get)
        }
      )
    }

    case x => log.warn("Unknown message: " + x)
  }

  override def preStart() {
    feed = loadFeed
  }

  override def postStop() {
    storeFeed(feed)
  }
}

object FeedActor {
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
    new FeedActor(descriptor, parser, httpClient, aggregate)
  }
}