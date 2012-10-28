package actors

import akka.actor.{Props, ActorSystem}
import models.rss.Rss
import models.atom.Atom
import models.FeedsConfiguration
import actors.FeedConfigurationActor.SetupFeeds

object FeedActorSystem {
  private var feedSystem: Option[ActorSystem] = None

  val feeds = Map(
    "programming" -> List(
      // Rss -> "https://markatta.com/johan/blog/?feed=rss2",
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
      // Rss -> "http://markatta.com/mary/funrun/?feed=rss2",
      Rss -> "http://www.sjuktgalen.se/feed/"
    )
  )

  def start() {
    feedSystem = Some(ActorSystem("feeds"))
    feedSystem.foreach { system =>
      val config = FeedsConfiguration("johan", feeds)
      val feedsActor = Some(system.actorOf(Props[FeedConfigurationActor], config.name))
      feedsActor.foreach(_ ! SetupFeeds(config))
    }
  }

  def stop() {
    feedSystem.foreach(_.shutdown())
  }

  def actorSystem: ActorSystem = feedSystem.getOrElse(throw new IllegalStateException("Feed actor system not started?"))

}
