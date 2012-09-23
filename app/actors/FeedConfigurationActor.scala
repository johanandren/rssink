package actors

import akka.actor._
import collection._
import play.api.libs.concurrent.Akka._
import scala.Seq
import akka.routing.RoundRobinRouter
import models.FeedsConfiguration

/**
 * Manages/supervises each set of feed actors
 */
class FeedConfigurationActor extends Actor with PlayActorLogging {

  val httpClientPoolSize = 4

  var httpClientRouter: Option[ActorRef] = None

  var configurations: Map[FeedsConfiguration, ConfigurationActors] = Map()


  override def preStart() {
    // TODO move http client router out of here?
    // http fetch actors
    httpClientRouter = Some(context.actorOf(Props(new HttpClientActor).withRouter(
      RoundRobinRouter(httpClientPoolSize))
    ))
  }

  def receive = {
    case SetupFeeds(configuration) => {
      log.info("Setting up feed configuration " + configuration)
      val running = setup(configuration)
      configurations += configuration -> running
    }

    case TearDownFeeds(configuration) => {
      log.info("Tearing down configuration " + configuration)
      configurations.get(configuration).foreach(running =>
        (running.aggregates.values ++ running.feeds).foreach(_ ! Kill)
      )
    }

    case TearDownAllConfigurations => {
      log.info("Tearing down all configurations")
      configurations.keys.foreach(self ! TearDownFeeds(_))
    }
  }

  def setup(configuration: FeedsConfiguration): ConfigurationActors = {

    val feeds = configuration.feeds

    // one aggregate per category keyed with the name from the configuration
    val aggregateActors: Map[String, ActorRef] = feeds.keys.map { name =>
      (name, context.actorOf(Props(new AggregateActor(name)), name))
    }(breakOut)

    // one actor per feed, which knows of its aggregate
    val feedActors: Seq[ActorRef] =
      feeds.flatMap { case (key, feedsForKey) =>
        val aggregateForKey = aggregateActors(key)
        feedsForKey.map { case (feedType, url) =>

          import FilterActor.Filters._
          val filter = context.actorOf(Props(new FilterActor(aggregateForKey, Seq(removeImages, removeIframes, removeObjects))))


          context.actorOf(Props(FeedActor(url, feedType, httpClientRouter.get, filter)), "feed-" + url.hashCode)
        }
      }(breakOut)

    ConfigurationActors(aggregateActors, feedActors)
  }

}

case class ConfigurationActors(aggregates: Map[String, ActorRef], feeds: Seq[ActorRef])