package actors

import scala.xml._
import play.api._
import akka.actor.Actor
import play.api.libs.ws.WS
import java.util.concurrent.TimeUnit._

class HttpClientActor extends Actor {

  val log = Logger("actor.http-client")

  def receive = {
    case FetchFeed(feed) => {
      log.debug("Fetching feed: " + feed)
      val fetchSender = sender

      WS.url(feed.url).get().map { response =>
        val xml = XML.loadString(response.body)
        log.debug("Got feed contents, sending back to " + sender)
        fetchSender ! FeedFetched(xml)
      }.orTimeout("Timeout getting feed " + feed, 20, SECONDS)
    }

    case x => log.warn("Unknown message: " + x)
  }
}
