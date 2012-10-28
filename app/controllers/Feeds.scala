package controllers

import play.api.mvc._
import akka.pattern._
import play.api.libs.concurrent._
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.ask
import play.api.Play.current
import models.atom.{AtomXmlFormat, AtomFeed}
import play.api.libs.iteratee.{Enumerator, Iteratee}
import akka.actor.ActorRef
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import actors.FeedActorSystem

object Feeds extends Controller {

  import actors.AggregateActor._
  import actors.EntryEventSource._
  import JsonFormats._

  implicit val timeout = Timeout(10 seconds)

  def system = FeedActorSystem.actorSystem

  def actorFor(user: String, feedKey: String): ActorRef = {
    val actorPath = "/user/" + user + "/" + feedKey
    system.actorFor(actorPath)
  }

  // async fetch of feed
  private def fetchFeed(user: String, feedKey: String): Promise[Either[String, AtomFeed]] = {
    val actor = actorFor(user, feedKey)
    (actor ? GetFeed).mapTo[Feed].asPromise.map { reply =>
       Right[String, AtomFeed](reply.feed)
    }
  }

  def feed(user: String, feedKey: String) = Action { implicit request =>
    AsyncResult {
      val actor = actorFor(user, feedKey)

      (actor ? GetSummary).asPromise.map { case Summary(title, subtitle) =>
         Ok(views.html.feed(title, subtitle, user, feedKey))
      }

    }
  }

  def rssFeed(user: String, feedKey: String) = Action {
    AsyncResult {
      fetchFeed(user, feedKey).map(_.fold(
        errorMsg => Ok(errorMsg),
        feed => Ok(AtomXmlFormat.write(feed))))
    }
  }

  // websocket stream of log items
  def feedStream(user: String, feedKey: String) = WebSocket.async[JsValue] { request =>
    val actor = actorFor(user, feedKey)

    (actor ? RegisterListener).asPromise.map {
      case ListenerRegistered(entryEnumerator) =>
        val in = Iteratee.foreach[JsValue](println).mapDone { _ =>
          actor ! UnregisterListener(entryEnumerator)
        }

        val out = entryEnumerator.map(entry => toJson(entry))

        (in, out)
    }
  }

}
