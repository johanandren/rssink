package actors

import akka.actor.{ActorLogging, Actor}
import models.{FeedType, RssAtomAdapter, FeedDescriptor}
import models.atom.{Atom, AtomXmlFormat, AtomFeed}
import actors.FeedParser.ParseFeed
import xml.{XML, NodeSeq}
import models.rss.{Rss, RssXmlReader}


object FeedParser {

  // messages
  case class ParseFeed(descriptor: FeedDescriptor, feedBody: String)
  case class FeedParsed(descriptor: FeedDescriptor, feed: AtomFeed)

  /** feed parser, parses XML NodeSeq into AtomFeed */
  type Parser = NodeSeq => Either[RuntimeException, AtomFeed]

  val rssParser: Parser = xml => RssXmlReader.parse(xml).fold(
    error => Left(error),
    success => Right(RssAtomAdapter.convert(success))
  )

  val atomParser: Parser = AtomXmlFormat.parse(_)

}


class FeedParser extends Actor with ActorLogging {

  import FeedParser._

  def receive = {

    case ParseFeed(descriptor, feedBody) =>
      log.debug("Parsing feed " + descriptor.feedName)
      val xml = XML.loadString(feedBody)
      val parser = parserFor(descriptor.feedType)
      val parseResult = parser(xml)

      parseResult.fold(
        error => throw error,
        atomFeed => sender ! FeedParsed(descriptor, atomFeed)
      )

  }


  def parserFor(feedType: FeedType): Parser = feedType match {
    case Rss => rssParser
    case Atom => atomParser
  }
}
