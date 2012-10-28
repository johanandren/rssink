import actors.{FeedActorSystem, FeedConfigurationActor}
import akka.actor.{ActorSystem, Props, ActorRef}
import models.FeedsConfiguration
import play.api.libs.concurrent.Akka
import models.atom.Atom
import models.rss.Rss
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings {

  val feedActorSystem = FeedActorSystem

  override def onStart(app: Application) {
    feedActorSystem.start()
  }

  override def onStop(app: Application) {
    feedActorSystem.stop()
  }
}
