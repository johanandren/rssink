package actors

import scala.xml._
import play.api._
import akka.actor._
import akka.actor.SupervisorStrategy._
import http.Status._
import play.api.libs.ws.WS
import java.util.concurrent.TimeUnit._
import java.util.concurrent.TimeoutException
import actors.TimeoutException

/**
 * Accepts the FetchFeed message to fetch the XML of a feed, will return a FeedFetched or
 * a failure to the sender.
 */
class HttpClientActor
  extends Actor
  with PlayActorLogging {

  val worker = context.actorOf(Props[HttpWorker])

  def receive = {
    case message: FetchFeed => worker forward message
    case x => log.warn("Unknown message: " + x)
  }

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3) {
    case _ :TimeoutException => Resume
    case x => {
      log.warn("Exception contacting http server", x)
      Resume
    }
  }

}

class HttpWorker
  extends Actor
  with PlayActorLogging {

  def receive = {
    case FetchFeed(feed) => {
      log.debug("Fetching feed: " + feed)

      val result = WS.url(feed.url).get().map { response =>
        response.status match {
          case OK => response.xml

          case code => throw new UnexpectedResponseCodeException("Unexpected HTTP response code: " + code)
        }
      }.orTimeout(new TimeoutException("Timeout getting feed " + feed), 20, SECONDS)
       .await.get

      result.fold(
        sender ! FeedFetched(_),
        throw _
      )
    }
  }
}
