package controllers

import play.api.mvc._
import actors._
import akka.pattern._
import play.api.libs.concurrent._
import akka.util.Timeout
import akka.util.duration._
import play.api.Play.current
import models.atom.{AtomXmlFormat, AtomFeed}

object Feeds extends Controller {

  private def fetchFeed(feedKey: String): Promise[Either[String, AtomFeed]] = {
    implicit val timeout = Timeout(10 seconds)
    ActorConfig.aggregateActors.get(feedKey).map { actor =>
      actor.ask(GetFeed).mapTo[Feed].asPromise.map { reply =>
        Right[String, AtomFeed](reply.feed)
      }
    } getOrElse {
      Akka.future(Left[String, AtomFeed]("Unknown feed key: " + feedKey))
    }
  }

  def feed(feedKey: String) = Action {
    AsyncResult {
      fetchFeed(feedKey).map(_.fold(
        errorMsg => Ok(errorMsg),
        feed => Ok(views.html.feed(feed))))
    }
  }

  def rssFeed(feedKey: String) = Action {
    AsyncResult {
      fetchFeed(feedKey).map(_.fold(
        errorMsg => Ok(errorMsg),
        feed => Ok(AtomXmlFormat.write(feed))))
    }
  }

}
