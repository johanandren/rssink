import actors.FeedConfigurationActor
import akka.actor.{Props, ActorRef}
import models.FeedsConfiguration
import play.api.libs.concurrent.Akka
import models.atom.Atom
import models.rss.Rss
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings {

  import FeedConfigurationActor._

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

  var feedsActor: Option[ActorRef] = None

  override def onStart(app: Application) {
    val system = Akka.system(app)
    val config = FeedsConfiguration("johan", feeds)
    feedsActor = Some(system.actorOf(Props[FeedConfigurationActor], config.name))
    feedsActor.foreach(_ ! SetupFeeds(config))
  }

  override def onStop(app: Application) {
    feedsActor.foreach(_ ! TearDownAllConfigurations)
  }
}
