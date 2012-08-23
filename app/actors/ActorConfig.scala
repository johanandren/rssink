package actors

import models.rss.Rss
import models.atom.{Id, AtomFeed, Atom}
import akka.actor.{Props, ActorRef}
import akka.util.duration._
import play.api.Play.current
import play.api.libs.concurrent.Akka.system
import collection._
import scala.Seq
import akka.routing.RoundRobinRouter

object ActorConfig {

  val feeds = Map(
    "programming" -> List(
      Rss -> "https://markatta.com/johan/blog/?feed=rss2",
      Rss -> "http://feeds.macrumors.com/MacRumors-All",
      Atom -> "http://www.planetscala.com/atom.xml",
      Atom -> "http://planet.jboss.org/xml/all?type=atom"
    ),
    "food" -> List(
      Rss -> "http://thevgun.com/feed/",
      Atom -> "http://www.goodstore.se/?feed=atom",
      Rss -> "http://veganmage.se/feed/"
    ),
    "friends" -> List(
      Rss -> "http://markatta.com/mary/funrun/?feed=rss2",
      Rss -> "http://www.sjuktgalen.se/feed/"
    )
  )

  // one aggregate per category keyed with the name from the configuration
  lazy val aggregateActors: Map[String, ActorRef] = feeds.keys.map { name =>
      (name, system.actorOf(Props(new AggregateActor(name))))
    }(breakOut)

  def init() {
    // http fetch actors
    val httpClientPoolSize = 4
    val httpClientRouter = system.actorOf(Props(new HttpClientActor).withRouter(
      RoundRobinRouter(httpClientPoolSize))
    )

    // one actor per feed, which knows of its aggregate
    val feedActors: Seq[ActorRef] =
      feeds.flatMap { case (key, feedsForKey) =>
        val aggregateForKey = aggregateActors(key)
        feedsForKey.map { case (feedType, url) =>
          system.actorOf(Props(FeedActor(url, feedType, httpClientRouter, aggregateForKey)))
        } map { actor =>
        // schedule updates every 10 minutes
          system.scheduler.schedule(1 seconds, 10 minutes, actor, PerformUpdate)
          actor
        }
      }(breakOut)

  }

}
