package actors

import play.api._
import akka.actor._
import akka.actor.SupervisorStrategy._
import http.Status._
import play.api.libs.ws.WS
import java.util.concurrent.TimeUnit._
import java.util.concurrent.TimeoutException
import akka.routing.RoundRobinRouter
import javax.net.ssl.SSLHandshakeException

object HttpClientActor {

  // messages
  case class FetchHttpUrl(url: String)
  case class HttpResponseBody(body: String)

  final class TimeoutException(msg: String) extends RuntimeException(msg)
  final class UnexpectedResponseCodeException(msg: String) extends RuntimeException(msg)

}

/**
 * Accepts the FetchFeed message to fetch the XML of a feed, will return a FeedFetched or
 * a failure to the sender.
 */
class HttpClientActor extends Actor with ActorLogging {

  import HttpClientActor._

  val workerCount = 5
  val workers = context.actorOf(Props[HttpWorker].withRouter(RoundRobinRouter(workerCount)), "Router")

  def receive = {

    case message: FetchHttpUrl => workers forward message

    case x => log.warning("Unknown message: " + x)

  }

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3) {

    case _ :HttpClientActor.TimeoutException => Resume

    case _: SSLHandshakeException => Restart

    case x =>
      log.warning("Exception contacting http server", x)
      Resume

  }

}

class HttpWorker extends Actor with ActorLogging {

  import HttpClientActor._

  def receive = {

    case FetchHttpUrl(url) =>
      log.debug("Fetching url: " + url)

      val result = WS.url(url).get().map { response =>

        if (response.status == OK) response.body
        else throw new UnexpectedResponseCodeException("Unexpected HTTP response code: " + response.status)

      }.orTimeout(new HttpClientActor.TimeoutException("Timeout getting feed " + url), 20, SECONDS)
       .await.get

      result.fold(
        sender ! HttpResponseBody(_),
        throw _
      )

  }

}