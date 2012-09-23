package controllers

import play.api.mvc._
import akka.pattern._
import play.api.libs.concurrent._
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.ask
import play.api.Play.current
import play.api.libs.concurrent.Akka.system
import models.atom.{AtomXmlFormat, AtomFeed}
import akka.actor._
import actors.{Feed, GetFeed}

object Feeds extends Controller {

  private def fetchFeed(user: String, feedKey: String): Promise[Either[String, AtomFeed]] = {
    implicit val timeout = Timeout(10 seconds)
    val actorPath = "/user/" + user + "/" + feedKey
    val future = (system.actorFor(actorPath) ask GetFeed)

    future.mapTo[Feed].asPromise.map { reply =>
       Right[String, AtomFeed](reply.feed)
    }
  }

  def feed(user: String, feedKey: String) = Action {
    AsyncResult {
      fetchFeed(user, feedKey).map(_.fold(
        errorMsg => Ok(errorMsg),
        feed => Ok(views.html.feed(feed))))
    }
  }

  def rssFeed(user: String, feedKey: String) = Action {
    AsyncResult {
      fetchFeed(user, feedKey).map(_.fold(
        errorMsg => Ok(errorMsg),
        feed => Ok(AtomXmlFormat.write(feed))))
    }
  }

}
